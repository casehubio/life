import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface ActivityEntry {
  workItemId: string;
  title: string;
  domain: string;
  status: string;
  createdAt: string;
  completedAt: string | null;
  outcome: string | null;
}

interface TaskEntry {
  workItemId: string;
  title: string;
  domain: string;
  status: string;
  urgency: string;
  daysOverdue: number | null;
  expiresAt: string | null;
}

interface ExternalActor {
  id: string;
  name: string;
  actorType: string;
  contactMethod: string;
  contactValue: string;
  createdAt: string;
  gdprErasedAt: string | null;
  trustProfile: { globalScore: number | null; dimensionScores: Record<string, number>; capabilityScores: Record<string, number> };
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

type PeopleTab = 'details' | 'trust' | 'activity' | 'tasks' | 'gdpr';

@customElement('people-view')
export class PeopleView extends LitElement {
  static override styles = css`
    :host { display: flex; height: 100%; gap: 1px; background: var(--pages-neutral-4, #d4d4d4); }
    .list-pane {
      width: 320px;
      flex-shrink: 0;
      background: var(--pages-neutral-1, #fafafa);
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .search {
      padding: var(--pages-space-3, 12px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .search input {
      width: 100%;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      outline: none;
      box-sizing: border-box;
    }
    .search input:focus { border-color: var(--pages-accent-7, #818cf8); }
    .people-list {
      flex: 1;
      overflow-y: auto;
    }
    .person {
      display: flex;
      align-items: center;
      gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px) var(--pages-space-4, 16px);
      cursor: pointer;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .person:hover { background: var(--pages-neutral-2, #f5f5f5); }
    .person[data-selected] { background: var(--pages-accent-3, #e0e7ff); }
    .person-avatar {
      width: 36px; height: 36px;
      border-radius: 50%;
      background: var(--pages-neutral-4, #d4d4d4);
      display: flex; align-items: center; justify-content: center;
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      flex-shrink: 0;
    }
    .person-info { flex: 1; min-width: 0; }
    .person-name {
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .person-contact {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .detail-pane {
      flex: 1;
      background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto;
      padding: var(--pages-space-5, 20px);
    }
    .detail-header {
      display: flex; align-items: center; gap: var(--pages-space-4, 16px);
      margin-bottom: var(--pages-space-5, 20px);
      padding-bottom: var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .detail-avatar {
      width: 56px; height: 56px;
      border-radius: 50%;
      background: var(--pages-neutral-4, #d4d4d4);
      display: flex; align-items: center; justify-content: center;
      font-size: var(--pages-font-size-xl, 20px);
      color: var(--pages-neutral-9, #525252);
    }
    .detail-name {
      font-size: var(--pages-font-size-xl, 20px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
    .detail-meta {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-8, #737373);
    }
    .tabs {
      display: flex;
      gap: var(--pages-space-1, 4px);
      margin-bottom: var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      padding-bottom: var(--pages-space-2, 8px);
    }
    .tab {
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      cursor: pointer;
      border-radius: var(--pages-radius-md, 6px);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .tab:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .tab[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3);
      font-weight: 500;
    }
    .field {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .field-label { color: var(--pages-neutral-8, #737373); }
    .field-value { color: var(--pages-neutral-12, #111); font-weight: 500; }
    .trust-bar {
      display: flex; align-items: center; gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-2, 8px) 0;
    }
    .trust-track {
      flex: 1; height: 6px;
      background: var(--pages-neutral-3, #f0f0f0);
      border-radius: 3px; overflow: hidden;
    }
    .trust-fill { height: 100%; border-radius: 3px; background: var(--pages-green-9, #16a34a); }
    .empty-detail {
      display: flex; align-items: center; justify-content: center; height: 100%;
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-base, 15px);
    }
    .erased { opacity: 0.5; }
    .erased-badge {
      font-size: var(--pages-font-size-xs, 12px);
      background: var(--pages-red-3, #fee2e2);
      color: var(--pages-red-11, #b91c1c);
      padding: 1px 6px; border-radius: 4px;
    }
    .activity-item {
      padding: var(--pages-space-3, 12px) 0;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .activity-title {
      font-size: var(--pages-font-size-sm, 14px); font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .activity-meta {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      display: flex; gap: var(--pages-space-3, 12px);
      margin-top: var(--pages-space-1, 4px);
    }
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
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px);
      text-align: center;
      padding: var(--pages-space-8, 32px);
    }
    .gdpr-section {
      padding: var(--pages-space-4, 16px) 0;
    }
    .gdpr-info {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      margin-bottom: var(--pages-space-4, 16px);
    }
    .gdpr-btn {
      padding: var(--pages-space-2, 8px) var(--pages-space-4, 16px);
      border: 1px solid var(--pages-red-7, #ef4444);
      border-radius: var(--pages-radius-md, 6px);
      background: var(--pages-red-3, #fee2e2);
      color: var(--pages-red-11, #b91c1c);
      font-size: var(--pages-font-size-sm, 14px);
      cursor: pointer;
      transition: background var(--pages-duration-fast, 120ms);
    }
    .gdpr-btn:hover { background: var(--pages-red-4, #fecaca); }
    .gdpr-btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .outcome-badge {
      font-size: var(--pages-font-size-xs, 12px);
      padding: 1px 6px; border-radius: 4px;
      background: var(--pages-neutral-3, #f0f0f0);
    }
    .count-badge {
      font-size: var(--pages-font-size-xs, 12px);
      background: var(--pages-neutral-3, #f0f0f0);
      color: var(--pages-neutral-9, #525252);
      padding: 1px 6px; border-radius: 8px; margin-left: 4px;
    }
  `;

  @state() private _actors: ExternalActor[] = [];
  @state() private _filtered: ExternalActor[] = [];
  @state() private _selected: ExternalActor | null = null;
  @state() private _activeTab: PeopleTab = 'details';
  @state() private _search = '';
  @state() private _activity: ActivityEntry[] = [];
  @state() private _tasks: TaskEntry[] = [];
  @state() private _activityLoading = false;
  @state() private _tasksLoading = false;
  @state() private _erasing = false;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/external-actors');
      if (!res.ok) return;
      const data = await res.json();
      this._actors = data.items ?? data;
      this._filtered = this._actors;
      if (this._actors.length > 0) this._selected = this._actors[0];
    } catch (e) { console.error(e); }
  }

  private _selectActor(a: ExternalActor): void {
    this._selected = a;
    this._activeTab = 'details';
    this._activity = [];
    this._tasks = [];
  }

  private async _fetchActivity(id: string): Promise<void> {
    this._activityLoading = true;
    try {
      const res = await fetch(`/external-actors/${id}/activity?size=20`);
      if (!res.ok) { this._activity = []; return; }
      const data = await res.json();
      this._activity = data.items ?? [];
    } catch (e) { console.error(e); this._activity = []; }
    finally { this._activityLoading = false; }
  }

  private async _fetchTasks(id: string): Promise<void> {
    this._tasksLoading = true;
    try {
      const res = await fetch(`/external-actors/${id}/tasks`);
      if (!res.ok) { this._tasks = []; return; }
      const data = await res.json();
      this._tasks = Array.isArray(data) ? data : (data.items ?? []);
    } catch (e) { console.error(e); this._tasks = []; }
    finally { this._tasksLoading = false; }
  }

  private _setTab(tab: PeopleTab): void {
    this._activeTab = tab;
    if (!this._selected) return;
    if (tab === 'activity' && this._activity.length === 0) this._fetchActivity(this._selected.id);
    if (tab === 'tasks' && this._tasks.length === 0) this._fetchTasks(this._selected.id);
  }

  private async _erasePersonalData(): Promise<void> {
    if (!this._selected || this._erasing) return;
    this._erasing = true;
    try {
      const res = await fetch(`/external-actors/${this._selected.id}/personal-data`, { method: 'DELETE' });
      if (res.ok) await this._fetch();
    } catch (e) { console.error(e); }
    finally { this._erasing = false; }
  }

  private _onSearch(e: Event): void {
    this._search = (e.target as HTMLInputElement).value.toLowerCase();
    this._filtered = this._actors.filter(a => a.name.toLowerCase().includes(this._search));
  }

  private _initials(name: string): string {
    return name.split(/\s+/).map(w => w[0]).join('').slice(0, 2).toUpperCase();
  }

  private _formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  override render() {
    return html`
      <div class="list-pane">
        <div class="search">
          <input type="text" placeholder="Search people…" @input=${this._onSearch} />
        </div>
        <div class="people-list">
          ${this._filtered.map(a => html`
            <div class="person ${a.gdprErasedAt ? 'erased' : ''}"
                 ?data-selected=${this._selected?.id === a.id}
                 @click=${() => this._selectActor(a)}>
              <div class="person-avatar">${this._initials(a.name)}</div>
              <div class="person-info">
                <div class="person-name">${a.name} ${a.gdprErasedAt ? html`<span class="erased-badge">ERASED</span>` : ''}</div>
                <div class="person-contact">${a.contactValue}</div>
              </div>
            </div>
          `)}
        </div>
      </div>
      <div class="detail-pane">
        ${this._selected ? this._renderDetail(this._selected) : html`<div class="empty-detail">Select a person</div>`}
      </div>
    `;
  }

  private _renderDetail(a: ExternalActor) {
    return html`
      <div class="detail-header">
        <div class="detail-avatar">${this._initials(a.name)}</div>
        <div>
          <div class="detail-name">${a.name}</div>
          <div class="detail-meta">${a.contactMethod} · ${a.contactValue}</div>
        </div>
      </div>
      <div class="tabs">
        ${(['details', 'trust', 'activity', 'tasks', 'gdpr'] as PeopleTab[]).map(t => html`
          <span class="tab" ?data-active=${this._activeTab === t}
                @click=${() => this._setTab(t)}>
            ${t === 'gdpr' ? 'GDPR' : t[0].toUpperCase() + t.slice(1)}
            ${t === 'activity' && this._activity.length > 0 ? html`<span class="count-badge">${this._activity.length}</span>` : nothing}
            ${t === 'tasks' && this._tasks.length > 0 ? html`<span class="count-badge">${this._tasks.length}</span>` : nothing}
          </span>
        `)}
      </div>
      ${this._activeTab === 'details' ? this._renderDetails(a) : nothing}
      ${this._activeTab === 'trust' ? this._renderTrust(a) : nothing}
      ${this._activeTab === 'activity' ? this._renderActivity() : nothing}
      ${this._activeTab === 'tasks' ? this._renderTasksTab() : nothing}
      ${this._activeTab === 'gdpr' ? this._renderGdpr(a) : nothing}
    `;
  }

  private _renderDetails(a: ExternalActor) {
    return html`
      <div class="field"><span class="field-label">Type</span><span class="field-value">${a.actorType.replace(/_/g, ' ')}</span></div>
      <div class="field"><span class="field-label">Contact</span><span class="field-value">${a.contactMethod}</span></div>
      <div class="field"><span class="field-label">Value</span><span class="field-value">${a.contactValue}</span></div>
      <div class="field"><span class="field-label">Since</span><span class="field-value">${new Date(a.createdAt).toLocaleDateString()}</span></div>
      ${a.gdprErasedAt ? html`<div class="field"><span class="field-label">GDPR Erased</span><span class="field-value">${new Date(a.gdprErasedAt).toLocaleDateString()}</span></div>` : ''}
    `;
  }

  private _renderTrust(a: ExternalActor) {
    const tp = a.trustProfile;
    if (!tp.globalScore && Object.keys(tp.dimensionScores).length === 0) {
      return html`<p style="color: var(--pages-neutral-8, #737373); font-size: 14px;">No trust data available yet</p>`;
    }
    return html`
      ${tp.globalScore != null ? html`
        <div class="field"><span class="field-label">Global Score</span><span class="field-value">${(tp.globalScore * 100).toFixed(0)}%</span></div>
        <div class="trust-bar">
          <div class="trust-track"><div class="trust-fill" style="width: ${tp.globalScore * 100}%"></div></div>
        </div>
      ` : ''}
      ${Object.entries(tp.dimensionScores).map(([dim, score]) => html`
        <div class="field"><span class="field-label">${dim.replace(/-/g, ' ')}</span><span class="field-value">${(score * 100).toFixed(0)}%</span></div>
      `)}
    `;
  }

  private _renderActivity() {
    if (this._activityLoading) return html`<div class="empty-state">Loading activity…</div>`;
    if (this._activity.length === 0) return html`<div class="empty-state">No activity recorded</div>`;
    return html`
      ${this._activity.map(a => html`
        <div class="activity-item">
          <div class="activity-title">${a.title}</div>
          <div class="activity-meta">
            <span>${DOMAIN_LABELS[a.domain] ?? a.domain}</span>
            <span>${a.status.toLowerCase()}</span>
            ${a.outcome ? html`<span class="outcome-badge">${a.outcome}</span>` : nothing}
            <span>${this._formatDate(a.createdAt)}</span>
          </div>
        </div>
      `)}
    `;
  }

  private _renderTasksTab() {
    if (this._tasksLoading) return html`<div class="empty-state">Loading tasks…</div>`;
    if (this._tasks.length === 0) return html`<div class="empty-state">No tasks assigned</div>`;
    return html`
      ${this._tasks.map(t => html`
        <div class="task-item">
          <div class="urgency-bar" style="background: ${URGENCY_COLORS[t.urgency] ?? '#a3a3a3'}"></div>
          <div class="task-content">
            <div class="task-title">${t.title}</div>
            <div class="task-meta">
              <span>${DOMAIN_LABELS[t.domain] ?? t.domain}</span>
              <span>${t.status.toLowerCase()}</span>
              ${t.daysOverdue != null ? html`<span style="color: var(--pages-red-9, #dc2626)">${t.daysOverdue}d overdue</span>` : nothing}
            </div>
          </div>
        </div>
      `)}
    `;
  }

  private _renderGdpr(a: ExternalActor) {
    if (a.gdprErasedAt) {
      return html`
        <div class="gdpr-section">
          <div class="field"><span class="field-label">GDPR Erased</span><span class="field-value">${this._formatDate(a.gdprErasedAt)}</span></div>
          <div class="gdpr-info" style="margin-top: 12px">Personal data has been erased in compliance with GDPR Art. 17. Trust scores and anonymised audit records are retained.</div>
        </div>
      `;
    }
    return html`
      <div class="gdpr-section">
        <div class="gdpr-info">
          Erasing personal data removes name, contact details, and associated memory records.
          Trust scores and anonymised ledger entries are retained for audit compliance.
          This action cannot be undone.
        </div>
        <button class="gdpr-btn" ?disabled=${this._erasing} @click=${this._erasePersonalData}>
          ${this._erasing ? 'Erasing…' : 'Erase Personal Data'}
        </button>
      </div>
    `;
  }
}
