import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

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
  `;

  @state() private _actors: ExternalActor[] = [];
  @state() private _filtered: ExternalActor[] = [];
  @state() private _selected: ExternalActor | null = null;
  @state() private _activeTab = 'details';
  @state() private _search = '';

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
    } catch { /* empty */ }
  }

  private _onSearch(e: Event): void {
    this._search = (e.target as HTMLInputElement).value.toLowerCase();
    this._filtered = this._actors.filter(a => a.name.toLowerCase().includes(this._search));
  }

  private _initials(name: string): string {
    return name.split(/\s+/).map(w => w[0]).join('').slice(0, 2).toUpperCase();
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
                 @click=${() => { this._selected = a; }}>
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
        ${['details', 'trust', 'activity'].map(t => html`
          <span class="tab" ?data-active=${this._activeTab === t}
                @click=${() => { this._activeTab = t; }}>${t[0].toUpperCase() + t.slice(1)}</span>
        `)}
      </div>
      ${this._activeTab === 'details' ? this._renderDetails(a) : ''}
      ${this._activeTab === 'trust' ? this._renderTrust(a) : ''}
      ${this._activeTab === 'activity' ? html`<p style="color: var(--pages-neutral-8, #737373); font-size: 14px;">Activity timeline coming soon</p>` : ''}
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
}
