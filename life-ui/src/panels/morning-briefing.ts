import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface BriefingItem {
  text: string;
  domain: string;
  type: string;
}

interface BriefingResponse {
  greeting: string;
  actionCount: number;
  items: BriefingItem[];
}

const DOMAIN_ICONS: Record<string, string> = {
  HOUSEHOLD: '🏠',
  HEALTH: '🏥',
  FINANCE: '💰',
  FAMILY_SCHEDULING: '📅',
  TRAVEL: '✈️',
  LEGAL: '⚖️',
  CONTRACTOR_COORDINATION: '🔧',
  ELDER_CARE: '❤️',
};

@customElement('life-morning-briefing')
export class MorningBriefing extends LitElement {
  static override styles = css`
    :host {
      display: block;
    }

    .card {
      background: var(--pages-neutral-1, #fafafa);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-lg, 8px);
      padding: var(--pages-space-5, 20px);
    }

    .greeting {
      font-size: var(--pages-font-size-xl, 20px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
      margin: 0 0 var(--pages-space-1, 4px) 0;
    }

    .subtitle {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      margin: 0 0 var(--pages-space-4, 16px) 0;
    }

    .items {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-2, 8px);
    }

    .item {
      display: flex;
      align-items: center;
      gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-11, #262626);
      transition: background var(--pages-duration-fast, 120ms);
      cursor: pointer;
    }

    .item:hover {
      background: var(--pages-neutral-3, #f0f0f0);
    }

    .item[data-type="sla-breach"] {
      color: var(--pages-red-11, #b91c1c);
    }

    .item[data-type="action"] {
      color: var(--pages-amber-11, #92400e);
    }

    .icon {
      flex-shrink: 0;
      width: 20px;
      text-align: center;
    }

    .empty {
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px);
      font-style: italic;
    }

    .loading {
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px);
    }

    .error {
      color: var(--pages-red-11, #b91c1c);
      font-size: var(--pages-font-size-sm, 14px);
    }
  `;

  @state() private _briefing: BriefingResponse | null = null;
  @state() private _loading = true;
  @state() private _error = '';

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetchBriefing();
  }

  private async _fetchBriefing(): Promise<void> {
    try {
      const res = await fetch('/dashboard/briefing');
      if (!res.ok) throw new Error(`${res.status}`);
      this._briefing = await res.json();
      this._error = '';
    } catch (e) {
      this._error = 'Could not load briefing';
    } finally {
      this._loading = false;
    }
  }

  override render() {
    if (this._loading) return html`<div class="card"><p class="loading">Loading briefing…</p></div>`;
    if (this._error) return html`<div class="card"><p class="error">${this._error}</p></div>`;
    if (!this._briefing) return html``;

    const { greeting, actionCount, items } = this._briefing;
    const subtitle = actionCount > 0
      ? `${actionCount} item${actionCount > 1 ? 's' : ''} need${actionCount === 1 ? 's' : ''} your attention today.`
      : 'Nothing urgent today.';

    return html`
      <div class="card">
        <p class="greeting">${greeting}</p>
        <p class="subtitle">${subtitle}</p>
        ${items.length > 0
          ? html`<div class="items">${items.map(item => html`
              <div class="item" data-type=${item.type}>
                <span class="icon">${DOMAIN_ICONS[item.domain] ?? '📋'}</span>
                <span>${item.text}</span>
              </div>
            `)}</div>`
          : html`<p class="empty">All clear — enjoy your day.</p>`}
      </div>
    `;
  }
}
