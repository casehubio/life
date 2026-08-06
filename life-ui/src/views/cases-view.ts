import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { LifeEventController, CASE_EVENT_TYPES } from '../events/life-event-controller.js';

interface LifeCase {
  caseId: string;
  id: string;
  caseType: string;
  domain: string;
  status: string;
  createdAt: string;
  completedAt: string | null;
  engineCaseId: string | null;
}

interface WorkItem {
  workItemId: string;
  title: string;
  description: string;
  status: string;
  domain: string;
  urgency: string;
  daysOverdue: number | null;
  createdAt: string;
  expiresAt: string | null;
}

const DOMAIN_ICONS: Record<string, string> = {
  HOUSEHOLD: '\u{1F3E0}', HEALTH: '\u{1F3E5}', FINANCE: '\u{1F4B0}',
  FAMILY_SCHEDULING: '\u{1F4C5}', TRAVEL: '✈️', LEGAL: '⚖️',
  CONTRACTOR_COORDINATION: '\u{1F527}', ELDER_CARE: '❤️',
};

const DOMAIN_LABELS: Record<string, string> = {
  HOUSEHOLD: 'Household', HEALTH: 'Health', FINANCE: 'Finance',
  FAMILY_SCHEDULING: 'Family', TRAVEL: 'Travel', LEGAL: 'Legal',
  CONTRACTOR_COORDINATION: 'Contractor', ELDER_CARE: 'Elder Care',
};

const CASE_LABELS: Record<string, string> = {
  CONTRACTOR_COORDINATION: 'Contractor Coordination',
  CARE_COORDINATION: 'Care Coordination',
  TRAVEL_PLAN: 'Travel Plan',
  APPOINTMENT_CYCLE: 'Appointment Cycle',
  FINANCIAL_REVIEW: 'Financial Review',
  HOME_MAINTENANCE: 'Home Maintenance',
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'var(--pages-green-9, #16a34a)',
  COMPLETED: 'var(--pages-neutral-8, #737373)',
  FAILED: 'var(--pages-red-9, #dc2626)',
};

const URGENCY_COLORS: Record<string, string> = {
  OVERDUE: 'var(--pages-red-9, #dc2626)',
  DUE_SOON: 'var(--pages-amber-9, #d97706)',
  NORMAL: 'var(--pages-neutral-5, #a3a3a3)',
  NO_DEADLINE: 'var(--pages-neutral-5, #a3a3a3)',
};

type DetailTab = 'overview' | 'tasks' | 'audit';

@customElement('cases-view')
export class CasesView extends LitElement {
  private _events = new LifeEventController(this, {
    types: CASE_EVENT_TYPES,
    onEvent: () => this._fetch(),
  });

  static override styles = css`
    :host { display: flex; height: 100%; gap: 1px; background: var(--pages-neutral-4, #d4d4d4); }
    .list-pane {
      width: 360px; flex-shrink: 0;
      background: var(--pages-neutral-1, #fafafa);
      display: flex; flex-direction: column; overflow: hidden;
    }
    .filters {
      display: flex; flex-wrap: wrap; gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-3, 12px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .filter-row { display: flex; gap: var(--pages-space-1, 4px); width: 100%; }
    .filter-btn {
      padding: var(--pages-space-1, 4px) var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      background: none; cursor: pointer;
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      transition: all var(--pages-duration-fast, 120ms);
    }
    .filter-btn:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .filter-btn[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      border-color: var(--pages-accent-7, #818cf8);
      color: var(--pages-accent-11, #3730a3);
    }
    .domain-select {
      flex: 1; padding: var(--pages-space-1, 4px) var(--pages-space-2, 8px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      background: none; cursor: pointer;
    }
    .case-list { flex: 1; overflow-y: auto; }
    .case-item {
      display: flex; align-items: center; gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px) var(--pages-space-4, 16px);
      cursor: pointer;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .case-item:hover { background: var(--pages-neutral-2, #f5f5f5); }
    .case-item[data-selected] { background: var(--pages-accent-3, #e0e7ff); }
    .case-icon { font-size: var(--pages-font-size-lg, 16px); flex-shrink: 0; }
    .case-info { flex: 1; min-width: 0; }
    .case-name {
      font-size: var(--pages-font-size-sm, 14px); font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .case-meta {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      display: flex; gap: var(--pages-space-2, 8px); align-items: center;
    }
    .status-dot {
      display: inline-block; width: 8px; height: 8px;
      border-radius: 50%; flex-shrink: 0;
    }
    .detail-pane {
      flex: 1; background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto; display: flex; flex-direction: column;
    }
    .detail-header {
      padding: var(--pages-space-5, 20px);
      padding-bottom: var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .detail-title {
      font-size: var(--pages-font-size-xl, 20px); font-weight: 600;
      color: var(--pages-neutral-12, #111);
      display: flex; align-items: center; gap: var(--pages-space-3, 12px);
    }
    .detail-subtitle {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-8, #737373);
      margin-top: var(--pages-space-1, 4px);
    }
    .field {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .status-badge {
      font-size: var(--pages-font-size-xs, 12px);
      padding: 2px 10px; border-radius: 10px; font-weight: 500;
    }
    .tabs {
      display: flex; gap: var(--pages-space-1, 4px);
      padding: var(--pages-space-2, 8px) var(--pages-space-5, 20px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .tab {
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      cursor: pointer; border-radius: var(--pages-radius-md, 6px);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .tab:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .tab[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3); font-weight: 500;
    }
    .tab-content {
      flex: 1; overflow-y: auto;
      padding: var(--pages-space-5, 20px);
    }
    .field-label { color: var(--pages-neutral-8, #737373); }
    .field-value { color: var(--pages-neutral-12, #111); font-weight: 500; }
    .task-item {
      display: flex; gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px) 0;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .urgency-bar { width: 3px; border-radius: 2px; flex-shrink: 0; }
    .task-content { flex: 1; min-width: 0; }
    .task-title {
      font-size: var(--pages-font-size-sm, 14px); font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .task-meta {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      display: flex; gap: var(--pages-space-3, 12px);
      margin-top: var(--pages-space-1, 4px);
    }
    .empty-state {
      display: flex; align-items: center; justify-content: center;
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-base, 15px);
      padding: var(--pages-space-8, 32px);
    }
    .empty-detail {
      display: flex; align-items: center; justify-content: center; height: 100%;
      color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-base, 15px);
    }
    .placeholder {
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px);
      text-align: center;
      padding: var(--pages-space-8, 32px) var(--pages-space-5, 20px);
    }
    .placeholder-icon { font-size: 32px; margin-bottom: var(--pages-space-3, 12px); }
    .count-badge {
      font-size: var(--pages-font-size-xs, 12px);
      background: var(--pages-neutral-3, #f0f0f0);
      color: var(--pages-neutral-9, #525252);
      padding: 1px 6px; border-radius: 8px; margin-left: 4px;
    }
  `;

  @state() private _cases: LifeCase[] = [];
  @state() private _filtered: LifeCase[] = [];
  @state() private _selected: LifeCase | null = null;
  @state() private _statusFilter: string | null = null;
  @state() private _domainFilter: string | null = null;
  @state() private _activeTab: DetailTab = 'overview';
  @state() private _tasks: WorkItem[] = [];
  @state() private _tasksLoading = false;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/life-cases?size=50');
      if (!res.ok) return;
      const data = await res.json();
      this._cases = data.items;
      this._applyFilter();
      if (!this._selected && this._filtered.length > 0) this._selected = this._filtered[0];
    } catch { /* empty */ }
  }

  private _applyFilter(): void {
    this._filtered = this._cases.filter(c => {
      if (this._statusFilter && c.status !== this._statusFilter) return false;
      if (this._domainFilter && c.domain !== this._domainFilter) return false;
      return true;
    });
  }

  private _setStatusFilter(status: string | null): void {
    this._statusFilter = this._statusFilter === status ? null : status;
    this._applyFilter();
  }

  private _setDomainFilter(e: Event): void {
    const val = (e.target as HTMLSelectElement).value;
    this._domainFilter = val || null;
    this._applyFilter();
  }

  private _selectCase(c: LifeCase): void {
    this._selected = c;
    this._activeTab = 'overview';
    this._tasks = [];
  }

  private async _fetchTasks(domain: string): Promise<void> {
    this._tasksLoading = true;
    try {
      const res = await fetch(`/pending-actions?domain=${domain}&size=50`);
      if (!res.ok) { this._tasks = []; return; }
      const data = await res.json();
      this._tasks = data.items ?? [];
    } catch { this._tasks = []; }
    finally { this._tasksLoading = false; }
  }

  private _setTab(tab: DetailTab): void {
    this._activeTab = tab;
    if (tab === 'tasks' && this._selected && this._tasks.length === 0) {
      this._fetchTasks(this._selected.domain);
    }
  }

  private _caseId(c: LifeCase): string { return c.caseId ?? c.id; }

  private _formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private _formatDateTime(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  private _formatAge(iso: string): string {
    const days = Math.round((Date.now() - new Date(iso).getTime()) / 86_400_000);
    if (days === 0) return 'today';
    if (days === 1) return '1 day';
    return `${days} days`;
  }

  private _uniqueDomains(): string[] {
    return [...new Set(this._cases.map(c => c.domain))].sort();
  }

  override render() {
    return html`
      <div class="list-pane">
        <div class="filters">
          <div class="filter-row">
            ${['ACTIVE', 'COMPLETED', 'FAILED'].map(s => html`
              <button class="filter-btn" ?data-active=${this._statusFilter === s}
                      @click=${() => this._setStatusFilter(s)}>${s.toLowerCase()}</button>
            `)}
          </div>
          <select class="domain-select" @change=${this._setDomainFilter}>
            <option value="">All domains</option>
            ${this._uniqueDomains().map(d => html`
              <option value=${d} ?selected=${this._domainFilter === d}>
                ${DOMAIN_ICONS[d] ?? ''} ${DOMAIN_LABELS[d] ?? d}
              </option>
            `)}
          </select>
        </div>
        <div class="case-list">
          ${this._filtered.length === 0
            ? html`<div class="empty-state">No cases match filters</div>`
            : this._filtered.map(c => html`
              <div class="case-item" ?data-selected=${this._selected && this._caseId(this._selected) === this._caseId(c)}
                   @click=${() => this._selectCase(c)}>
                <span class="case-icon">${DOMAIN_ICONS[c.domain] ?? '\u{1F4CB}'}</span>
                <div class="case-info">
                  <div class="case-name">${CASE_LABELS[c.caseType] ?? c.caseType}</div>
                  <div class="case-meta">
                    <span class="status-dot" style="background: ${STATUS_COLORS[c.status] ?? '#999'}"></span>
                    <span>${c.status.toLowerCase()}</span>
                    <span>${this._formatAge(c.createdAt)}</span>
                  </div>
                </div>
              </div>
            `)}
        </div>
      </div>
      <div class="detail-pane">
        ${this._selected ? this._renderDetail(this._selected) : html`<div class="empty-detail">Select a case</div>`}
      </div>
    `;
  }

  private _renderDetail(c: LifeCase) {
    const statusColor = STATUS_COLORS[c.status] ?? '#999';
    return html`
      <div class="detail-header">
        <div class="detail-title">
          <span>${DOMAIN_ICONS[c.domain] ?? '\u{1F4CB}'}</span>
          ${CASE_LABELS[c.caseType] ?? c.caseType}
          <span class="status-badge" style="background: ${statusColor}22; color: ${statusColor}">
            ${c.status.toLowerCase()}
          </span>
        </div>
        <div class="detail-subtitle">${DOMAIN_LABELS[c.domain] ?? c.domain}</div>
      </div>
      <div class="tabs">
        ${(['overview', 'tasks', 'audit'] as DetailTab[]).map(t => html`
          <span class="tab" ?data-active=${this._activeTab === t}
                @click=${() => this._setTab(t)}>
            ${t[0].toUpperCase() + t.slice(1)}
            ${t === 'tasks' && this._tasks.length > 0 ? html`<span class="count-badge">${this._tasks.length}</span>` : nothing}
          </span>
        `)}
      </div>
      <div class="tab-content">
        ${this._activeTab === 'overview' ? this._renderOverview(c) : nothing}
        ${this._activeTab === 'tasks' ? this._renderTasks() : nothing}
        ${this._activeTab === 'audit' ? this._renderAuditPlaceholder() : nothing}
      </div>
    `;
  }

  private _renderOverview(c: LifeCase) {
    return html`
      <div class="field"><span class="field-label">Case Type</span><span class="field-value">${CASE_LABELS[c.caseType] ?? c.caseType}</span></div>
      <div class="field"><span class="field-label">Domain</span><span class="field-value">${DOMAIN_LABELS[c.domain] ?? c.domain}</span></div>
      <div class="field"><span class="field-label">Status</span><span class="field-value">${c.status}</span></div>
      <div class="field"><span class="field-label">Started</span><span class="field-value">${this._formatDate(c.createdAt)}</span></div>
      ${c.completedAt ? html`
        <div class="field"><span class="field-label">Completed</span><span class="field-value">${this._formatDate(c.completedAt)}</span></div>
      ` : nothing}
      <div class="field"><span class="field-label">Duration</span><span class="field-value">${this._formatAge(c.createdAt)}</span></div>
      ${c.engineCaseId ? html`
        <div class="field"><span class="field-label">Engine Case</span><span class="field-value" style="font-family: monospace; font-size: 12px">${c.engineCaseId}</span></div>
      ` : nothing}
    `;
  }

  private _renderTasks() {
    if (this._tasksLoading) return html`<div class="empty-state">Loading tasks…</div>`;
    if (this._tasks.length === 0) return html`<div class="empty-state">No tasks in this domain</div>`;
    return html`
      ${this._tasks.map(t => html`
        <div class="task-item">
          <div class="urgency-bar" style="background: ${URGENCY_COLORS[t.urgency] ?? '#a3a3a3'}"></div>
          <div class="task-content">
            <div class="task-title">${t.title}</div>
            <div class="task-meta">
              <span>${t.status.toLowerCase()}</span>
              ${t.daysOverdue != null ? html`<span style="color: var(--pages-red-9, #dc2626)">${t.daysOverdue}d overdue</span>` : nothing}
              <span>${this._formatDateTime(t.expiresAt)}</span>
            </div>
          </div>
        </div>
      `)}
    `;
  }

  private _renderAuditPlaceholder() {
    return html`
      <div class="placeholder">
        <div class="placeholder-icon">\u{1F4DC}</div>
        <div>Audit trail, timeline, and CBR similarity will be available when engine cases are active.</div>
      </div>
    `;
  }
}
