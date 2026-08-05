import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { LifeEventController, INBOX_EVENT_TYPES } from '../events/life-event-controller.js';

interface PendingAction {
  workItemId: string;
  title: string;
  description: string;
  status: string;
  domain: string;
  candidateGroups: string;
  createdAt: string;
  expiresAt: string | null;
  urgency: 'OVERDUE' | 'DUE_SOON' | 'NORMAL' | 'NO_DEADLINE';
  daysOverdue: number | null;
}

interface PagedResponse {
  items: PendingAction[];
  page: number;
  size: number;
  totalCount: number;
}

const URGENCY_INDICATOR: Record<string, string> = {
  OVERDUE: '⚠',
  DUE_SOON: '⏰',
  NORMAL: '○',
  NO_DEADLINE: '·',
};

const DOMAIN_LABELS: Record<string, string> = {
  HOUSEHOLD: 'Household',
  HEALTH: 'Health',
  FINANCE: 'Finance',
  FAMILY_SCHEDULING: 'Family',
  TRAVEL: 'Travel',
  LEGAL: 'Legal',
  CONTRACTOR_COORDINATION: 'Contractor',
  ELDER_CARE: 'Elder Care',
};

@customElement('life-action-items')
export class ActionItems extends LitElement {
  private _events = new LifeEventController(this, {
    types: INBOX_EVENT_TYPES,
    onEvent: () => this._fetchActions(),
  });

  static override styles = css`
    :host {
      display: block;
    }

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--pages-space-3, 12px);
    }

    h3 {
      margin: 0;
      font-size: var(--pages-font-size-base, 15px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

    .count {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }

    .items {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-1, 4px);
    }

    .item {
      display: flex;
      align-items: flex-start;
      gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px);
      border-radius: var(--pages-radius-md, 6px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      background: var(--pages-neutral-1, #fafafa);
      cursor: pointer;
      transition: border-color var(--pages-duration-fast, 120ms),
                  box-shadow var(--pages-duration-fast, 120ms);
    }

    .item:hover {
      border-color: var(--pages-accent-7, #818cf8);
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    }

    .indicator {
      flex-shrink: 0;
      font-size: var(--pages-font-size-lg, 16px);
      line-height: 1.2;
    }

    .content {
      flex: 1;
      min-width: 0;
    }

    .title {
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
      color: var(--pages-neutral-12, #111);
      margin-bottom: var(--pages-space-1, 4px);
    }

    .meta {
      display: flex;
      gap: var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }

    .item[data-urgency="OVERDUE"] {
      border-left: 3px solid var(--pages-red-9, #dc2626);
    }

    .item[data-urgency="OVERDUE"] .indicator {
      color: var(--pages-red-9, #dc2626);
    }

    .item[data-urgency="DUE_SOON"] {
      border-left: 3px solid var(--pages-amber-9, #d97706);
    }

    .item[data-urgency="DUE_SOON"] .indicator {
      color: var(--pages-amber-9, #d97706);
    }

    .empty {
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px);
      text-align: center;
      padding: var(--pages-space-5, 20px);
    }

    .loading, .error {
      font-size: var(--pages-font-size-sm, 14px);
      padding: var(--pages-space-4, 16px);
    }

    .loading { color: var(--pages-neutral-8, #737373); }
    .error { color: var(--pages-red-11, #b91c1c); }
  `;

  @state() private _actions: PendingAction[] = [];
  @state() private _totalCount = 0;
  @state() private _loading = true;
  @state() private _error = '';

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetchActions();
  }

  private async _fetchActions(): Promise<void> {
    try {
      const res = await fetch('/pending-actions?size=10&dueSoonHours=24');
      if (!res.ok) throw new Error(`${res.status}`);
      const data: PagedResponse = await res.json();
      this._actions = data.items;
      this._totalCount = data.totalCount;
      this._error = '';
    } catch {
      this._error = 'Could not load actions';
    } finally {
      this._loading = false;
    }
  }

  private _navigateToInbox(id: string): void {
    window.location.hash = `inbox?item=${id}`;
  }

  override render() {
    if (this._loading) return html`<p class="loading">Loading actions…</p>`;
    if (this._error) return html`<p class="error">${this._error}</p>`;

    return html`
      <div class="header">
        <h3>Action Items</h3>
        ${this._totalCount > 0 ? html`<span class="count">${this._totalCount} total</span>` : ''}
      </div>
      ${this._actions.length > 0
        ? html`<div class="items">${this._actions.map(action => html`
            <div class="item"
                 data-urgency=${action.urgency}
                 @click=${() => this._navigateToInbox(action.workItemId)}>
              <span class="indicator">${URGENCY_INDICATOR[action.urgency]}</span>
              <div class="content">
                <div class="title">${action.title}</div>
                <div class="meta">
                  <span>${DOMAIN_LABELS[action.domain] ?? action.domain}</span>
                  ${action.daysOverdue != null ? html`<span>${action.daysOverdue}d overdue</span>` : ''}
                  ${action.urgency === 'DUE_SOON' && action.expiresAt
                    ? html`<span>Due ${this._formatRelative(action.expiresAt)}</span>` : ''}
                </div>
              </div>
            </div>
          `)}</div>`
        : html`<p class="empty">No pending actions</p>`}
    `;
  }

  private _formatRelative(iso: string): string {
    const diff = new Date(iso).getTime() - Date.now();
    const hours = Math.round(diff / 3_600_000);
    if (hours <= 0) return 'now';
    if (hours < 24) return `in ${hours}h`;
    return `in ${Math.round(hours / 24)}d`;
  }
}
