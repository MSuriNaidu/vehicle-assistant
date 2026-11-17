import { useEffect, useRef, useState } from "react";
import type { FormEvent, KeyboardEvent } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  ArrowUp,
  ArrowUpRight,
  Check,
  CircleAlert,
  CircleHelp,
  Clock3,
  Gauge,
  LifeBuoy,
  MessageSquareText,
  Plus,
  ShieldCheck,
  Sparkles,
  Wrench,
} from "lucide-react";

type Urgency = "STOP_NOW" | "DRIVE_CAREFULLY" | "SERVICE_SOON" | "ROUTINE";

type AdviceResponse = {
  urgency: Urgency | null;
  answer: string;
  safetyTriggered: boolean;
  safetyReason: string | null;
};

type Message =
  | { id: string; role: "user"; text: string; sentAt: Date }
  | { id: string; role: "assistant"; response: AdviceResponse; sentAt: Date };

const prompts = [
  {
    icon: Gauge,
    title: "Warning light",
    detail: "Understand a dashboard symbol",
    question:
      "My check-engine light is on. The car is a 2020 Hyundai i20 and it feels normal. What should I check first?",
  },
  {
    icon: ShieldCheck,
    title: "Safety concern",
    detail: "Get a clear immediate next step",
    question:
      "My brake pedal suddenly feels soft and sinks when I press it. I have stopped the car. What should I do?",
  },
  {
    icon: Wrench,
    title: "Unusual sound",
    detail: "Describe a noise or vibration",
    question:
      "My 2019 Maruti Suzuki Swift squeals when I brake. The brakes still work. What could be causing it?",
  },
];

const urgencyDetails: Record<Urgency, { label: string; className: string }> = {
  STOP_NOW: { label: "Stop driving", className: "stop" },
  DRIVE_CAREFULLY: { label: "Drive carefully", className: "caution" },
  SERVICE_SOON: { label: "Service soon", className: "service" },
  ROUTINE: { label: "Routine", className: "routine" },
};

function createId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

function formatTime(date: Date) {
  return date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState("");
  const [failedQuestion, setFailedQuestion] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const conversationRef = useRef<HTMLDivElement>(null);
  const hasConversation = messages.length > 0;

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = "0px";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  }, [draft]);

  useEffect(() => {
    conversationRef.current?.scrollTo({
      top: conversationRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, isSending]);

  async function sendMessage(text = draft, retry = false) {
    const question = text.trim();
    if (!question || isSending) return;

    setError("");
    setDraft("");
    setIsSending(true);
    if (!retry) {
      setMessages((current) => [
        ...current,
        { id: createId(), role: "user", text: question, sentAt: new Date() },
      ]);
    }

    try {
      const response = await fetch("/api/ai/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question }),
      });

      const body = await response.json().catch(() => null);
      if (!response.ok) {
        const message =
          typeof body?.message === "string"
            ? body.message
            : response.status === 429
              ? "The assistant is busy right now. Please wait a moment and try again."
              : "We couldn't get an answer right now. Please try again.";
        throw new Error(message);
      }

      const result = body as AdviceResponse;
      if (typeof result.answer !== "string" || !result.answer.trim()) {
        throw new Error("The assistant returned an empty answer. Please try again.");
      }

      setMessages((current) => [
        ...current,
        { id: createId(), role: "assistant", response: result, sentAt: new Date() },
      ]);
      setFailedQuestion("");
    } catch (requestError) {
      setFailedQuestion(question);
      setError(
        requestError instanceof TypeError
          ? "Couldn't reach the assistant. Check that the backend is running and try again."
          : requestError instanceof Error
            ? requestError.message
            : "Something went wrong. Please try again.",
      );
    } finally {
      setIsSending(false);
      textareaRef.current?.focus();
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void sendMessage();
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void sendMessage();
    }
  }

  function startNewChat() {
    if (isSending) return;
    setMessages([]);
    setDraft("");
    setError("");
    setFailedQuestion("");
    textareaRef.current?.focus();
  }

  return (
    <div className="app-frame">
      <aside className="side-rail">
        <a className="brand" href="#" aria-label="VigiDrive home" onClick={startNewChat}>
          <span className="brand-mark" aria-hidden="true">
            <Gauge size={19} strokeWidth={2.2} />
          </span>
          <span className="brand-copy">
            <strong>VigiDrive</strong>
            <small>VEHICLE CARE</small>
          </span>
        </a>

        <div className="rail-divider" />
        <div className="rail-section-label">WORKSPACE</div>
        <div className="rail-current">
          <MessageSquareText size={17} />
          <span>Assistant</span>
          <span className="active-indicator" />
        </div>

        <div className="rail-bottom">
          <div className="rail-safety-icon"><ShieldCheck size={17} /></div>
          <div className="rail-safety-title">Safety comes first.</div>
          <p>If a warning feels urgent, pull over safely and contact roadside assistance.</p>
          <div className="rail-divider" />
          <div className="rail-region"><span className="region-dot" /> INDIA <span>·</span> INR</div>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumb"><span>VIGIDRIVE</span><span className="crumb-slash">/</span><strong>Assistant</strong></div>
          <div className="topbar-actions">
            <div className="availability">PERSONAL ASSISTANT</div>
            <button className="new-chat-button" onClick={startNewChat} disabled={isSending}>
              <Plus size={15} />
              <span>New chat</span>
            </button>
          </div>
        </header>

        <main className={`workspace ${hasConversation ? "workspace-conversation" : ""}`}>
          <div className="workspace-heading">
            <div>
              <div className="section-eyebrow"><span /> PERSONAL VEHICLE SUPPORT</div>
              <h1>{hasConversation ? "Your conversation" : "Let’s get to the bottom of it."}</h1>
              <p>{hasConversation
                ? "Clear, practical guidance for your next step."
                : "Tell us what your car is doing. We’ll help you work out what to do next."}</p>
            </div>
            <div className="guidance-badge"><ShieldCheck size={15} /> Safety-first guidance</div>
          </div>

          <div className={`work-grid ${hasConversation ? "work-grid-chat" : ""}`}>
            <section className="chat-surface" aria-label="Vehicle assistant chat">
              <div className="chat-toolbar">
                <div className="assistant-id">
                  <div className="assistant-mark"><Sparkles size={16} /></div>
                  <div>
                    <strong>VigiDrive assistant</strong>
                    <span>Vehicle troubleshooting</span>
                  </div>
                  <span className="verified-mark" aria-label="Assistant"><Check size={10} /></span>
                </div>
                <div className="response-time"><Clock3 size={13} /> AI-assisted guidance</div>
              </div>

              <div className="conversation" ref={conversationRef} aria-live="polite">
                {!hasConversation ? (
                  <div className="empty-state">
                    <div className="welcome-block">
                      <div className="welcome-avatar"><Sparkles size={17} /></div>
                      <div>
                        <div className="message-label">YOUR VEHICLE ASSISTANT</div>
                        <h2>What’s happening with your car?</h2>
                        <p>Start with the symptoms. Add the make, model, year or any dashboard code if you know them.</p>
                      </div>
                    </div>
                    <div className="suggestions-heading">
                      <span>START WITH A COMMON TOPIC</span>
                      <span className="suggestions-rule" />
                    </div>
                    <div className="suggestion-list">
                      {prompts.map(({ icon: Icon, title, detail, question }) => (
                        <button className="suggestion-card" key={title} onClick={() => void sendMessage(question)}>
                          <span className="suggestion-icon"><Icon size={17} /></span>
                          <span className="suggestion-copy"><strong>{title}</strong><small>{detail}</small></span>
                          <ArrowUpRight size={16} className="suggestion-arrow" />
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="message-list">
                    {messages.map((message) => message.role === "user" ? (
                      <div className="user-message" key={message.id}>
                        <div className="user-bubble">{message.text}</div>
                        <div className="message-timestamp">YOU <span>·</span> {formatTime(message.sentAt)}</div>
                      </div>
                    ) : (
                      <AssistantMessage key={message.id} response={message.response} sentAt={message.sentAt} />
                    ))}
                    {isSending && <TypingIndicator />}
                  </div>
                )}
              </div>

              <div className="composer-wrap">
                {error && (
                  <div className="error-banner" role="alert">
                    <CircleAlert size={16} />
                    <span>{error}</span>
                    <button onClick={() => void sendMessage(failedQuestion, true)}>Try again</button>
                  </div>
                )}
                <form className="composer" onSubmit={onSubmit}>
                  <textarea
                    ref={textareaRef}
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onKeyDown={onKeyDown}
                    placeholder="Describe what you’ve noticed…"
                    aria-label="Describe your vehicle issue"
                    rows={1}
                    disabled={isSending}
                  />
                  <div className="composer-footer">
                    <span className="composer-hint">Be specific—when it happens, warning lights, sounds or codes.</span>
                    <div className="composer-tools">
                      <span className="enter-hint">SHIFT + ENTER FOR NEW LINE</span>
                      <button className="send-button" type="submit" disabled={!draft.trim() || isSending} aria-label="Send message">
                        {isSending ? <span className="send-spinner" /> : <ArrowUp size={17} />}
                      </button>
                    </div>
                  </div>
                </form>
                <div className="composer-disclaimer">
                  <ShieldCheck size={12} /> AI guidance can make mistakes. A qualified mechanic should inspect your car.
                </div>
              </div>
            </section>

            {!hasConversation && (
              <aside className="detail-rail">
                <section className="detail-card">
                  <div className="detail-overline">A BETTER ANSWER STARTS HERE</div>
                  <h3>What to include</h3>
                  <p>A few details help us give you more useful guidance.</p>
                  <ul className="detail-checklist">
                    <li><span>01</span><div><strong>Your vehicle</strong><small>Make, model and year</small></div></li>
                    <li><span>02</span><div><strong>What you noticed</strong><small>When it started and when it happens</small></div></li>
                    <li><span>03</span><div><strong>Any warning code</strong><small>OBD code, if you have one</small></div></li>
                  </ul>
                </section>
                <section className="safety-card">
                  <div className="safety-card-top"><ShieldCheck size={16} /><span>ON THE ROAD</span></div>
                  <h3>Trust your instincts.</h3>
                  <p>If the car feels unsafe, stop somewhere safe. Don’t wait for an online answer in an emergency.</p>
                  <div className="safety-card-footer"><LifeBuoy size={14} /> Contact local roadside assistance</div>
                </section>
                <div className="privacy-note"><ShieldCheck size={14} /><span>Share only details relevant to your vehicle question.</span></div>
              </aside>
            )}
          </div>

          <footer className="page-footer">
            <span>© 2026 VigiDrive</span>
            <span className="footer-note"><CircleHelp size={13} /> Helpful guidance, not a substitute for inspection</span>
            <span>Made for the road ahead</span>
          </footer>
        </main>
      </div>
    </div>
  );
}

function AssistantMessage({ response, sentAt }: { response: AdviceResponse; sentAt: Date }) {
  const urgency = response.urgency ? urgencyDetails[response.urgency] : null;
  return (
    <article className="assistant-message">
      <div className="welcome-avatar"><Sparkles size={15} /></div>
      <div className="assistant-message-body">
        <div className="assistant-message-meta"><strong>VigiDrive</strong><span>·</span>{formatTime(sentAt)}</div>
        <div className="response-card">
          {urgency && (
            <div className={`urgency-banner ${urgency.className}`}>
              <span className="urgency-dot" />
              <strong>{urgency.label}</strong>
              <span className="urgency-note">Based on what you’ve shared</span>
            </div>
          )}
          {response.safetyTriggered && (
            <div className="safety-notice">
              <ShieldCheck size={17} />
              <span><strong>Safety alert</strong>{response.safetyReason?.replaceAll("_", " ").toLowerCase()}</span>
            </div>
          )}
          <div className="markdown-answer">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{response.answer}</ReactMarkdown>
          </div>
        </div>
        <div className="response-footnote"><ShieldCheck size={13} /> Helpful guidance—not a substitute for a mechanic’s inspection.</div>
      </div>
    </article>
  );
}

function TypingIndicator() {
  return (
    <div className="assistant-message typing-message" role="status" aria-label="Assistant is thinking">
      <div className="welcome-avatar"><Sparkles size={15} /></div>
      <div className="typing-card">
        <div className="typing-label">PREPARING YOUR GUIDANCE</div>
        <div className="typing-dots"><span /><span /><span /></div>
      </div>
    </div>
  );
}

export default App;
