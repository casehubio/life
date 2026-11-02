import { LitElement, html, css, PropertyValues } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

interface ChatMessage {
  role: 'user' | 'avatar';
  content: string;
  timestamp: Date;
}

@customElement('life-conversation-panel')
export class ConversationPanel extends LitElement {

  @state() private messages: ChatMessage[] = [];
  @state() private inputText = '';
  @state() private isStreaming = false;
  @state() private isThinking = false;

  static override styles = css`
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
      box-sizing: border-box;
      font-family: var(--pages-font-family, system-ui);
      background: var(--pages-neutral-1, #fafafa);
      color: var(--pages-neutral-12, #111);
    }

    .header {
      display: flex;
      align-items: center;
      padding: var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      border-bottom: 1px solid var(--pages-neutral-4, #d4d4d4);
    }

    .messages {
      flex: 1;
      overflow-y: auto;
      padding: var(--pages-space-2, 8px);
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-2, 8px);
    }

    .msg {
      max-width: 90%;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      line-height: 1.45;
      word-wrap: break-word;
    }

    .msg.user {
      align-self: flex-end;
      background: var(--pages-blue-9, #2563eb);
      color: white;
      border-bottom-right-radius: 2px;
    }

    .msg.avatar {
      align-self: flex-start;
      background: var(--pages-neutral-3, #f0f0f0);
      border-bottom-left-radius: 2px;
    }

    .msg.avatar :first-child { margin-top: 0; }
    .msg.avatar :last-child { margin-bottom: 0; }
    .msg.avatar p { margin: 4px 0; }
    .msg.avatar code {
      background: var(--pages-neutral-4, #d4d4d4);
      padding: 1px 4px;
      border-radius: 3px;
      font-size: var(--pages-font-size-xs, 12px);
    }

    .thinking {
      align-self: flex-start;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      font-style: italic;
    }

    .input-area {
      display: flex;
      padding: var(--pages-space-2, 8px);
      border-top: 1px solid var(--pages-neutral-4, #d4d4d4);
      gap: var(--pages-space-2, 8px);
    }

    .input-area input {
      flex: 1;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-5, #c4c4c4);
      border-radius: var(--pages-radius-full, 9999px);
      font-size: var(--pages-font-size-sm, 14px);
      outline: none;
      background: var(--pages-neutral-1, #fafafa);
      color: var(--pages-neutral-12, #111);
    }

    .input-area input:focus {
      border-color: var(--pages-blue-8, #3b82f6);
    }

    .send-btn {
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border: none;
      border-radius: var(--pages-radius-full, 9999px);
      background: var(--pages-blue-9, #2563eb);
      color: white;
      cursor: pointer;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
    }

    .send-btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .empty {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--pages-neutral-7, #a3a3a3);
      font-size: var(--pages-font-size-sm, 14px);
      font-style: italic;
      padding: var(--pages-space-4, 16px);
      text-align: center;
    }
  `;

  override render() {
    return html`
      <div class="header">Avatar</div>
      ${this.messages.length === 0 && !this.isStreaming
        ? html`<div class="empty">Ask me anything about your household</div>`
        : html`
          <div class="messages">
            ${this.messages.map(msg => this._renderMessage(msg))}
            ${this.isThinking ? html`<div class="thinking">Thinking...</div>` : ''}
          </div>
        `}
      <div class="input-area">
        <input
          type="text"
          placeholder="Message..."
          .value=${this.inputText}
          @input=${(e: InputEvent) => { this.inputText = (e.target as HTMLInputElement).value; }}
          @keydown=${(e: KeyboardEvent) => { if (e.key === 'Enter' && !this.isStreaming) this._send(); }}
          ?disabled=${this.isStreaming}
        />
        <button class="send-btn" @click=${this._send} ?disabled=${this.isStreaming || !this.inputText.trim()}>
          Send
        </button>
      </div>
    `;
  }

  private _renderMessage(msg: ChatMessage) {
    if (msg.role === 'user') {
      return html`<div class="msg user">${msg.content}</div>`;
    }
    const rendered = DOMPurify.sanitize(marked.parse(msg.content) as string);
    return html`<div class="msg avatar">${unsafeHTML(rendered)}</div>`;
  }

  private async _send() {
    const text = this.inputText.trim();
    if (!text || this.isStreaming) return;

    this.messages = [...this.messages, { role: 'user', content: text, timestamp: new Date() }];
    this.inputText = '';
    this.isStreaming = true;
    this.isThinking = true;

    let avatarContent = '';
    let avatarAdded = false;

    try {
      const response = await fetch('/avatar/message', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: text,
      });

      if (!response.ok || !response.body) {
        throw new Error(`Request failed: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (!line.startsWith('data:')) continue;
          const data = line.slice(5).trim();
          if (!data) continue;

          try {
            const event = JSON.parse(data);
            if (event.text !== undefined) {
              this.isThinking = false;
              avatarContent += event.text;
              if (!avatarAdded) {
                this.messages = [...this.messages, { role: 'avatar', content: avatarContent, timestamp: new Date() }];
                avatarAdded = true;
              } else {
                const updated = [...this.messages];
                updated[updated.length - 1] = { ...updated[updated.length - 1], content: avatarContent };
                this.messages = updated;
              }
            }
          } catch { /* skip malformed SSE data */ }
        }
      }
    } catch {
      if (!avatarAdded) {
        this.messages = [...this.messages, {
          role: 'avatar',
          content: 'Sorry, something went wrong. Please try again.',
          timestamp: new Date(),
        }];
      }
    } finally {
      this.isStreaming = false;
      this.isThinking = false;
    }
  }

  protected override updated(_changedProperties: PropertyValues) {
    super.updated(_changedProperties);
    const messagesDiv = this.shadowRoot?.querySelector('.messages');
    if (messagesDiv) {
      messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
  }
}
