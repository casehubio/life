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
  urgency: string;
  daysOverdue: number | null;
}

const DOMAIN_LABELS: Record<string, string> = {
  HOUSEHOLD: 'Household', HEALTH: 'Health', FINANCE: 'Finance',
  FAMILY_SCHEDULING: 'Family', TRAVEL: 'Travel', LEGAL: 'Legal',
  CONTRACTOR_COORDINATION: 'Contractor', ELDER_CARE: 'Elder Care',
};

const URGENCY_COLORS: Record<string, string> = {
  OVERDUE: 'var(--pages-red-9, #dc2626)',
  DUE_SOON: 'var(--pages-amber-9, #d97706)',
  NORMAL: 'var(--pages-neutral-5, #a3a3a3)',
  NO_DEADLINE: 'var(--pages-neutral-5, #a3a3a3)',
};

@customElement('inbox-view')
export class InboxView extends LitElement {
  private _events = new LifeEventController(this, {
    types: INBOX_EVENT_TYPES,
    onEvent: () => this._fetch(),
  });

  static override styles = css`
    :host { display: flex; height: 100%; gap: 1px; background: var(--pages-neutral-4, #d4d4d4); }
    .list-pane {
      width: 400px; flex-shrink: 0;
      background: var(--pages-neutral-1, #fafafa);
      display: flex; flex-direction: column; overflow: hidden;
    }
    .list-header {
      padding: var(--pages-space-3, 12px) var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      display: flex; justify-content: space-between; align-items: center;
    }
    .list-header h3 {
      margin: 0; font-size: var(--pages-font-size-base, 15px);
      font-weight: 600; color: var(--pages-neutral-12, #111);
    }
    .list-count {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }
    .tabs {
      display: flex; gap: var(--pages-space-1, 4px);
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .tab {
      padding: var(--pages-space-1, 4px) var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      cursor: pointer; border-radius: var(--pages-radius-md, 6px);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .tab:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .tab[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3); font-weight: 500;
    }
    .item-list { flex: 1; overflow-y: auto; }
    .item {
      display: flex; gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px) var(--pages-space-4, 16px);
      cursor: pointer;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .item:hover { background: var(--pages-neutral-2, #f5f5f5); }
    .item[data-selected] { background: var(--pages-accent-3, #e0e7ff); }
    .urgency-bar {
      width: 3px; border-radius: 2px; flex-shrink: 0;
    }
    .item-content { flex: 1; min-width: 0; }
    .item-title {
      font-size: var(--pages-font-size-sm, 14px); font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .item-meta {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      display: flex; gap: var(--pages-space-3, 12px);
      margin-top: var(--pages-space-1, 4px);
    }
    .detail-pane {
      flex: 1; background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto; padding: var(--pages-space-5, 20px);
    }
    .detail-title {
      font-size: var(--pages-font-size-xl, 20px); font-weight: 600;
      color: var(--pages-neutral-12, #111);
      margin-bottom: var(--pages-space-2, 8px);
    }
    .detail-desc {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      margin-bottom: var(--pages-space-5, 20px);
      padding-bottom: var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .field {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .field-label { color: var(--pages-neutral-8, #737373); }
    .field-value { color: var(--pages-neutral-12, #111); font-weight: 500; }
    .urgency-badge {
      display: inline-block;
      padding: 1px 8px; border-radius: 10px;
      font-size: var(--pages-font-size-xs, 12px); font-weight: 500;
    }
    .empty-detail {
      display: flex; align-items: center; justify-content: center; height: 100%;
      color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-base, 15px);
    }
  `;

  @state() private _items: PendingAction[] = [];
  @state() private _selected: PendingAction | null = null;
  @state() private _activeTab = 'all';
  @state() private _totalCount = 0;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/pending-actions?size=50&dueSoonHours=24');
      if (!res.ok) return;
      const data = await res.json();
      this._items = data.items;
      this._totalCount = data.totalCount;
      if (this._items.length > 0) this._selected = this._items[0];
    } catch (e) { console.error(e); }
  }

  private get _filteredItems(): PendingAction[] {
    if (this._activeTab === 'overdue') return this._items.filter(i => i.urgency === 'OVERDUE');
    if (this._activeTab === 'due-soon') return this._items.filter(i => i.urgency === 'DUE_SOON');
    return this._items;
  }

  private _formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  override render() {
    const filtered = this._filteredItems;
    return html`
      <div class="list-pane">
        <div class="list-header">
          <h3>Inbox</h3>
          <span class="list-count">${this._totalCount} items</span>
        </div>
        <div class="tabs">
          ${[['all', 'All'], ['overdue', 'Overdue'], ['due-soon', 'Due Soon']].map(([key, label]) => html`
            <span class="tab" ?data-active=${this._activeTab === key}
                  @click=${() => { this._activeTab = key; }}>${label}</span>
          `)}
        </div>
        <div class="item-list">
          ${filtered.map(item => html`
            <div class="item" ?data-selected=${this._selected?.workItemId === item.workItemId}
                 @click=${() => { this._selected = item; }}>
              <div class="urgency-bar" style="background: ${URGENCY_COLORS[item.urgency]}"></div>
              <div class="item-content">
                <div class="item-title">${item.title}</div>
                <div class="item-meta">
                  <span>${DOMAIN_LABELS[item.domain] ?? item.domain}</span>
                  <span>${item.status.toLowerCase()}</span>
                  ${item.daysOverdue != null ? html`<span>${item.daysOverdue}d overdue</span>` : ''}
                </div>
              </div>
            </div>
          `)}
        </div>
      </div>
      <div class="detail-pane">
        ${this._selected ? this._renderDetail(this._selected) : html`<div class="empty-detail">Select an item</div>`}
      </div>
    `;
  }

  private _renderDetail(item: PendingAction) {
    return html`
      <div class="detail-title">${item.title}</div>
      <div class="detail-desc">${item.description}</div>
      <div class="field"><span class="field-label">Status</span><span class="field-value">${item.status}</span></div>
      <div class="field"><span class="field-label">Domain</span><span class="field-value">${DOMAIN_LABELS[item.domain] ?? item.domain}</span></div>
      <div class="field">
        <span class="field-label">Urgency</span>
        <span class="urgency-badge" style="background: ${URGENCY_COLORS[item.urgency]}22; color: ${URGENCY_COLORS[item.urgency]}">
          ${item.urgency.replace(/_/g, ' ')}
        </span>
      </div>
      <div class="field"><span class="field-label">Groups</span><span class="field-value">${item.candidateGroups}</span></div>
      <div class="field"><span class="field-label">Created</span><span class="field-value">${this._formatDate(item.createdAt)}</span></div>
      <div class="field"><span class="field-label">Expires</span><span class="field-value">${this._formatDate(item.expiresAt)}</span></div>
      ${item.daysOverdue != null ? html`
        <div class="field"><span class="field-label">Days Overdue</span><span class="field-value" style="color: var(--pages-red-9, #dc2626)">${item.daysOverdue}</span></div>
      ` : ''}
    `;
  }
}
