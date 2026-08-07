import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface PendingAction {
  workItemId: string;
  title: string;
  status: string;
  domain: string;
  urgency: string;
  daysOverdue: number | null;
}

const URGENCY_DOT: Record<string, string> = {
  OVERDUE: '\u{1F534}',
  DUE_SOON: '\u{1F7E1}',
  NORMAL: '⚪',
  NO_DEADLINE: '⚪',
};

@customElement('life-inbox-dock')
export class InboxDock extends LitElement {
  static override styles = css`
    :host {
      display: block;
      height: 100%;
      overflow-y: auto;
      padding: var(--pages-space-3, 12px);
      box-sizing: border-box;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--pages-space-3, 12px);
    }
    h3 {
      margin: 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
    .count {
      font-size: var(--pages-font-size-xs, 12px);
      background: var(--pages-red-3, #fee2e2);
      color: var(--pages-red-11, #b91c1c);
      padding: 1px 6px;
      border-radius: 10px;
      font-weight: 500;
    }
    .items {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-1, 4px);
    }
    .item {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-2, 8px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-11, #262626);
      cursor: pointer;
      transition: background var(--pages-duration-fast, 120ms);
    }
    .item:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .dot { flex-shrink: 0; font-size: 8px; }
    .title {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .view-all {
      display: block;
      text-align: center;
      padding: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-accent-11, #3730a3);
      cursor: pointer;
      margin-top: var(--pages-space-2, 8px);
    }
    .view-all:hover { text-decoration: underline; }
    .empty, .loading {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }
  `;

  @state() private _items: PendingAction[] = [];
  @state() private _totalCount = 0;
  @state() private _loading = true;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/pending-actions?size=5&dueSoonHours=24');
      if (!res.ok) return;
      const data = await res.json();
      this._items = data.items;
      this._totalCount = data.totalCount;
    } catch (e) { console.error(e); }
    finally { this._loading = false; }
  }

  override render() {
    if (this._loading) return html`<p class="loading">Loading…</p>`;
    return html`
      <div class="header">
        <h3>Inbox</h3>
        ${this._totalCount > 0 ? html`<span class="count">${this._totalCount}</span>` : ''}
      </div>
      ${this._items.length > 0
        ? html`
          <div class="items">
            ${this._items.map(item => html`
              <div class="item" @click=${() => { window.location.hash = 'inbox'; }}>
                <span class="dot">${URGENCY_DOT[item.urgency]}</span>
                <span class="title">${item.title}</span>
              </div>
            `)}
          </div>
          ${this._totalCount > 5 ? html`<a class="view-all" @click=${() => { window.location.hash = 'inbox'; }}>View all ${this._totalCount}</a>` : ''}
        `
        : html`<p class="empty">No pending items</p>`}
    `;
  }
}
