import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface Member {
  id: string;
  name: string;
  email: string;
  role: string;
  relationship: string;
  notificationChannel: string;
  notificationValue: string;
  joinedAt: string;
  active: boolean;
}

interface HouseholdData {
  id: string;
  name: string;
  timezone: string;
  jurisdiction: string;
}

type SettingsTab = 'members' | 'household' | 'templates' | 'voice';

@customElement('settings-view')
export class SettingsView extends LitElement {
  static override styles = css`
    :host { display: block; height: 100%; overflow-y: auto; padding: var(--pages-space-5, 20px); box-sizing: border-box; }
    h2 { font-size: var(--pages-font-size-xl, 20px); font-weight: 600; color: var(--pages-neutral-12, #111); margin: 0 0 var(--pages-space-4, 16px) 0; }
    .tabs {
      display: flex; gap: var(--pages-space-1, 4px);
      margin-bottom: var(--pages-space-5, 20px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      padding-bottom: var(--pages-space-2, 8px);
    }
    .tab {
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-9, #525252);
      cursor: pointer; border-radius: var(--pages-radius-md, 6px);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .tab:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .tab[data-active] { background: var(--pages-accent-3, #e0e7ff); color: var(--pages-accent-11, #3730a3); font-weight: 500; }
    .card {
      background: var(--pages-neutral-1, #fafafa); border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-lg, 8px); padding: var(--pages-space-5, 20px); max-width: 700px;
    }
    .member-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: var(--pages-space-3, 12px) 0;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .member-info { flex: 1; }
    .member-name { font-size: var(--pages-font-size-sm, 14px); font-weight: 500; color: var(--pages-neutral-12, #111); }
    .member-meta { font-size: var(--pages-font-size-xs, 12px); color: var(--pages-neutral-8, #737373); display: flex; gap: var(--pages-space-3, 12px); margin-top: 2px; }
    .field {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0; font-size: var(--pages-font-size-sm, 14px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .field-label { color: var(--pages-neutral-8, #737373); }
    .field-value { color: var(--pages-neutral-12, #111); font-weight: 500; }
    .placeholder-card {
      text-align: center; color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-sm, 14px); padding: var(--pages-space-8, 32px);
    }
    .badge {
      font-size: var(--pages-font-size-xs, 12px); padding: 1px 8px; border-radius: 10px;
      background: var(--pages-accent-3, #e0e7ff); color: var(--pages-accent-11, #3730a3);
    }
    .badge-inactive { background: var(--pages-neutral-3, #f0f0f0); color: var(--pages-neutral-8, #737373); }
    .empty-state { color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-sm, 14px); padding: var(--pages-space-5, 20px); text-align: center; }
  `;

  @state() private _tab: SettingsTab = 'members';
  @state() private _members: Member[] = [];
  @state() private _household: HouseholdData | null = null;
  @state() private _voiceAvailable = false;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetchAll();
  }

  private async _fetchAll(): Promise<void> {
    try {
      const [hRes, mRes, cRes] = await Promise.all([
        fetch('/household').then(r => r.ok ? r.json() : null),
        fetch('/household/members').then(r => r.ok ? r.json() : null),
        fetch('/household/capabilities').then(r => r.ok ? r.json() : null),
      ]);
      this._household = hRes;
      this._members = mRes ?? [];
      this._voiceAvailable = cRes?.voiceEnrollment ?? false;
    } catch (e) { console.error(e); }
  }

  override render() {
    return html`
      <h2>Settings</h2>
      <div class="tabs">
        ${(['members', 'household', 'templates', 'voice'] as SettingsTab[]).map(t => html`
          <span class="tab" ?data-active=${this._tab === t} @click=${() => this._tab = t}>
            ${t === 'members' ? 'Members' : t === 'household' ? 'Household' : t === 'templates' ? 'Templates' : 'Voice'}
          </span>
        `)}
      </div>
      <div class="card">
        ${this._tab === 'members' ? this._renderMembers() : nothing}
        ${this._tab === 'household' ? this._renderHousehold() : nothing}
        ${this._tab === 'templates' ? this._renderTemplates() : nothing}
        ${this._tab === 'voice' ? this._renderVoice() : nothing}
      </div>
    `;
  }

  private _renderMembers() {
    if (this._members.length === 0) return html`<div class="empty-state">No members added yet</div>`;
    return html`
      ${this._members.map(m => html`
        <div class="member-row">
          <div class="member-info">
            <div class="member-name">${m.name} <span class="badge ${m.active ? '' : 'badge-inactive'}">${m.role.replace('household-', '')}</span></div>
            <div class="member-meta">
              <span>${m.email ?? '—'}</span>
              <span>${m.relationship ?? ''}</span>
              ${m.notificationChannel ? html`<span>${m.notificationChannel}: ${m.notificationValue}</span>` : nothing}
            </div>
          </div>
        </div>
      `)}
    `;
  }

  private _renderHousehold() {
    if (!this._household) return html`<div class="empty-state">No household configured</div>`;
    return html`
      <div class="field"><span class="field-label">Name</span><span class="field-value">${this._household.name}</span></div>
      <div class="field"><span class="field-label">Timezone</span><span class="field-value">${this._household.timezone}</span></div>
      <div class="field"><span class="field-label">Jurisdiction</span><span class="field-value">${this._household.jurisdiction ?? '—'}</span></div>
    `;
  }

  private _renderTemplates() {
    return html`
      <div class="member-row"><div class="member-info"><div class="member-name">Health appointment</div><div class="member-meta"><span>48h expiry</span></div></div></div>
      <div class="member-row"><div class="member-info"><div class="member-name">Household task</div><div class="member-meta"><span>24h expiry</span></div></div></div>
      <div class="member-row"><div class="member-info"><div class="member-name">Contractor coordination</div><div class="member-meta"><span>72h expiry</span></div></div></div>
    `;
  }

  private _renderVoice() {
    if (this._voiceAvailable) {
      return html`<div class="placeholder-card">Voice enrollment UI — available when avatar integration is active</div>`;
    }
    return html`<div class="placeholder-card">Voice enrollment is not available yet. This feature will be enabled with avatar voice detection (#120).</div>`;
  }
}
