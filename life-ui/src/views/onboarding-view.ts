import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface MemberForm {
  name: string;
  email: string;
  role: string;
  relationship: string;
  notificationChannel: string;
  notificationValue: string;
}

@customElement('onboarding-view')
export class OnboardingView extends LitElement {
  static override styles = css`
    :host {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
    }
    .wizard {
      max-width: 600px;
      width: 100%;
      background: var(--pages-neutral-1, #fafafa);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-lg, 8px);
      padding: var(--pages-space-6, 24px);
    }
    h2 {
      margin: 0 0 var(--pages-space-2, 8px) 0;
      font-size: var(--pages-font-size-xl, 20px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
    .step-indicator {
      display: flex;
      gap: var(--pages-space-2, 8px);
      margin-bottom: var(--pages-space-5, 20px);
    }
    .step-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: var(--pages-neutral-4, #d4d4d4);
    }
    .step-dot[data-active] { background: var(--pages-accent-9, #6366f1); }
    .step-dot[data-done] { background: var(--pages-green-9, #16a34a); }
    .form-group {
      margin-bottom: var(--pages-space-4, 16px);
    }
    label {
      display: block;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
      color: var(--pages-neutral-11, #262626);
      margin-bottom: var(--pages-space-1, 4px);
    }
    input, select {
      width: 100%;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      box-sizing: border-box;
    }
    input:focus, select:focus { border-color: var(--pages-accent-7, #818cf8); outline: none; }
    .actions {
      display: flex;
      justify-content: space-between;
      margin-top: var(--pages-space-5, 20px);
    }
    .btn {
      padding: var(--pages-space-2, 8px) var(--pages-space-5, 20px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      cursor: pointer;
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      background: none;
      color: var(--pages-neutral-9, #525252);
    }
    .btn:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .btn-primary {
      background: var(--pages-accent-9, #6366f1);
      color: white;
      border-color: var(--pages-accent-9, #6366f1);
    }
    .btn-primary:hover { background: var(--pages-accent-10, #4f46e5); }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .member-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-3, #f0f0f0);
      border-radius: var(--pages-radius-md, 6px);
      margin-bottom: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-sm, 14px);
    }
    .member-name { font-weight: 500; color: var(--pages-neutral-12, #111); }
    .member-role { color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-xs, 12px); }
    .remove-btn {
      background: none; border: none; cursor: pointer;
      color: var(--pages-red-9, #dc2626); font-size: 14px;
    }
    .subtitle {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-8, #737373);
      margin-bottom: var(--pages-space-4, 16px);
    }
    .summary-item {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      font-size: var(--pages-font-size-sm, 14px);
    }
    .summary-label { color: var(--pages-neutral-8, #737373); }
    .summary-value { font-weight: 500; color: var(--pages-neutral-12, #111); }
    .error { color: var(--pages-red-9, #dc2626); font-size: var(--pages-font-size-sm, 14px); margin-top: var(--pages-space-2, 8px); }
  `;

  @state() private _step = 1;
  @state() private _householdName = '';
  @state() private _timezone = 'Europe/London';
  @state() private _jurisdiction = 'GB';
  @state() private _members: MemberForm[] = [];
  @state() private _newMember: MemberForm = { name: '', email: '', role: 'household-member', relationship: 'OTHER', notificationChannel: 'email', notificationValue: '' };
  @state() private _saving = false;
  @state() private _error = '';

  override render() {
    return html`
      <div class="wizard">
        <div class="step-indicator">
          ${[1,2,3,4,5].map(s => html`
            <div class="step-dot" ?data-active=${this._step === s} ?data-done=${this._step > s}></div>
          `)}
        </div>
        ${this._step === 1 ? this._renderStep1() : nothing}
        ${this._step === 2 ? this._renderStep2() : nothing}
        ${this._step === 3 ? this._renderStep3() : nothing}
        ${this._step === 4 ? this._renderStep4() : nothing}
        ${this._step === 5 ? this._renderStep5() : nothing}
        ${this._error ? html`<div class="error">${this._error}</div>` : nothing}
      </div>
    `;
  }

  private _renderStep1() {
    return html`
      <h2>Create your household</h2>
      <p class="subtitle">Give your household a name and set your location.</p>
      <div class="form-group">
        <label>Household name</label>
        <input type="text" .value=${this._householdName} placeholder="e.g. The Proctors"
          @input=${(e: Event) => this._householdName = (e.target as HTMLInputElement).value} />
      </div>
      <div class="form-group">
        <label>Timezone</label>
        <select .value=${this._timezone} @change=${(e: Event) => this._timezone = (e.target as HTMLSelectElement).value}>
          <option value="Europe/London">Europe/London</option>
          <option value="America/New_York">America/New York</option>
          <option value="America/Los_Angeles">America/Los Angeles</option>
          <option value="Asia/Tokyo">Asia/Tokyo</option>
          <option value="UTC">UTC</option>
        </select>
      </div>
      <div class="form-group">
        <label>Jurisdiction</label>
        <select .value=${this._jurisdiction} @change=${(e: Event) => this._jurisdiction = (e.target as HTMLSelectElement).value}>
          <option value="GB">United Kingdom</option>
          <option value="US">United States</option>
          <option value="IE">Ireland</option>
          <option value="AU">Australia</option>
        </select>
      </div>
      <div class="actions">
        <span></span>
        <button class="btn btn-primary" ?disabled=${!this._householdName || this._saving}
          @click=${this._createHousehold}>${this._saving ? 'Creating...' : 'Next'}</button>
      </div>
    `;
  }

  private _renderStep2() {
    return html`
      <h2>Add family members</h2>
      <p class="subtitle">Add people to your household. You can add more later.</p>
      ${this._members.map((m, i) => html`
        <div class="member-card">
          <div>
            <div class="member-name">${m.name}</div>
            <div class="member-role">${m.role} · ${m.email}</div>
          </div>
          <button class="remove-btn" @click=${() => { this._members = this._members.filter((_, idx) => idx !== i); }}>✕</button>
        </div>
      `)}
      <div class="form-group"><label>Name</label>
        <input type="text" .value=${this._newMember.name} @input=${(e: Event) => this._newMember = {...this._newMember, name: (e.target as HTMLInputElement).value}} /></div>
      <div class="form-group"><label>Email</label>
        <input type="email" .value=${this._newMember.email} @input=${(e: Event) => this._newMember = {...this._newMember, email: (e.target as HTMLInputElement).value}} /></div>
      <div class="form-group"><label>Role</label>
        <select .value=${this._newMember.role} @change=${(e: Event) => this._newMember = {...this._newMember, role: (e.target as HTMLSelectElement).value}}>
          <option value="household-member">Member</option>
          <option value="household-junior">Junior</option>
          <option value="household-admin">Admin</option>
        </select></div>
      <div class="form-group"><label>Relationship</label>
        <select .value=${this._newMember.relationship} @change=${(e: Event) => this._newMember = {...this._newMember, relationship: (e.target as HTMLSelectElement).value}}>
          <option value="PARENT">Parent</option>
          <option value="CHILD">Child</option>
          <option value="SPOUSE">Spouse</option>
          <option value="GUARDIAN">Guardian</option>
          <option value="OTHER">Other</option>
        </select></div>
      <button class="btn" ?disabled=${!this._newMember.name} @click=${this._addMember}>Add member</button>
      <div class="actions">
        <button class="btn" @click=${() => this._step = 1}>Back</button>
        <button class="btn btn-primary" ?disabled=${this._saving}
          @click=${this._submitMembers}>${this._saving ? 'Saving...' : this._members.length > 0 ? 'Next' : 'Skip'}</button>
      </div>
    `;
  }

  private _renderStep3() {
    return html`
      <h2>Add external contacts</h2>
      <p class="subtitle">Add your GP, school, contractors, solicitors. You can add more later from the People view.</p>
      <div class="actions">
        <button class="btn" @click=${() => this._step = 2}>Back</button>
        <button class="btn btn-primary" @click=${() => this._step = 4}>Skip for now</button>
      </div>
    `;
  }

  private _renderStep4() {
    return html`
      <h2>Task templates</h2>
      <p class="subtitle">These templates define the types of tasks your household can create. Default templates are pre-enabled.</p>
      <div class="member-card"><span class="member-name">Health appointment</span><span class="member-role">48h expiry</span></div>
      <div class="member-card"><span class="member-name">Household task</span><span class="member-role">24h expiry</span></div>
      <div class="member-card"><span class="member-name">Contractor coordination</span><span class="member-role">72h expiry</span></div>
      <div class="actions">
        <button class="btn" @click=${() => this._step = 3}>Back</button>
        <button class="btn btn-primary" @click=${() => this._step = 5}>Next</button>
      </div>
    `;
  }

  private _renderStep5() {
    return html`
      <h2>You're all set!</h2>
      <p class="subtitle">Your household is ready. Here's a summary.</p>
      <div class="summary-item"><span class="summary-label">Household</span><span class="summary-value">${this._householdName}</span></div>
      <div class="summary-item"><span class="summary-label">Timezone</span><span class="summary-value">${this._timezone}</span></div>
      <div class="summary-item"><span class="summary-label">Members added</span><span class="summary-value">${this._members.length}</span></div>
      <div class="actions">
        <span></span>
        <button class="btn btn-primary" @click=${() => window.location.hash = 'home'}>Go to Dashboard</button>
      </div>
    `;
  }

  private _addMember(): void {
    if (!this._newMember.name) return;
    this._members = [...this._members, { ...this._newMember }];
    this._newMember = { name: '', email: '', role: 'household-member', relationship: 'OTHER', notificationChannel: 'email', notificationValue: '' };
  }

  private async _createHousehold(): Promise<void> {
    this._saving = true;
    this._error = '';
    try {
      const res = await fetch('/onboarding/household', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: this._householdName, timezone: this._timezone, jurisdiction: this._jurisdiction }),
      });
      if (!res.ok) { this._error = 'Failed to create household'; return; }
      this._step = 2;
    } catch (e) { console.error(e); this._error = 'Network error'; }
    finally { this._saving = false; }
  }

  private async _submitMembers(): Promise<void> {
    if (this._members.length === 0) { this._step = 3; return; }
    this._saving = true;
    this._error = '';
    try {
      const res = await fetch('/onboarding/members', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this._members),
      });
      if (!res.ok) { this._error = 'Failed to add members'; return; }
      this._step = 3;
    } catch (e) { console.error(e); this._error = 'Network error'; }
    finally { this._saving = false; }
  }
}
