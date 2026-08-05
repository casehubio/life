import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@casehubio/pages-ui-tokens/dist/init.js';
import { applyTheme } from '@casehubio/blocks-ui-core';

type View = 'home' | 'inbox' | 'people' | 'cases' | 'journal';

const NAV_ITEMS: { view: View; label: string }[] = [
  { view: 'home', label: 'Dashboard' },
  { view: 'inbox', label: 'Inbox' },
  { view: 'people', label: 'People' },
  { view: 'cases', label: 'Cases' },
  { view: 'journal', label: 'Journal' },
];

@customElement('app-shell')
export class AppShell extends LitElement {
  @state() private currentView: View = 'home';
  @state() private _darkMode = false;

  static override styles = css`
    :host {
      display: flex;
      flex-direction: column;
      height: 100vh;
      font-family: var(--pages-font-family, system-ui);
    }

    nav {
      display: flex;
      align-items: center;
      gap: var(--pages-space-1, 4px);
      padding: var(--pages-space-2, 8px) var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-4, #d4d4d4);
      background: var(--pages-neutral-1, #fafafa);
    }

    .brand {
      font-weight: 600;
      font-size: var(--pages-font-size-lg, 16px);
      color: var(--pages-neutral-12, #111);
      margin-right: var(--pages-space-4, 16px);
    }

    nav a {
      text-decoration: none;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border-radius: var(--pages-radius-md, 6px);
      color: var(--pages-neutral-9, #525252);
      font-size: var(--pages-font-size-sm, 14px);
      cursor: pointer;
      transition: background var(--pages-duration-fast, 120ms) var(--pages-ease-out, ease-out);
    }

    nav a:hover {
      background: var(--pages-neutral-3, #f0f0f0);
    }

    nav a[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3);
      font-weight: 500;
    }

    .spacer { flex: 1; }

    .toolbar {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
    }

    .toolbar button {
      background: none;
      border: none;
      cursor: pointer;
      font-size: var(--pages-font-size-base, 15px);
      padding: var(--pages-space-1, 4px) var(--pages-space-2, 8px);
      border-radius: var(--pages-radius-md, 6px);
      color: var(--pages-neutral-9, #525252);
      transition: background var(--pages-duration-fast, 120ms);
    }

    .toolbar button:hover {
      background: var(--pages-neutral-3, #f0f0f0);
    }

    .user {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      padding: var(--pages-space-1, 4px) var(--pages-space-2, 8px);
    }

    main {
      flex: 1;
      overflow: auto;
      padding: var(--pages-space-5, 20px);
      background: var(--pages-neutral-2, #f5f5f5);
    }

    .placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: var(--pages-neutral-8, #737373);
      font-size: var(--pages-font-size-lg, 16px);
    }
  `;

  override connectedCallback(): void {
    super.connectedCallback();
    applyTheme('casehub-light');
    window.addEventListener('hashchange', this._onHashChange);
    this._syncViewFromHash();
  }

  override disconnectedCallback(): void {
    super.disconnectedCallback();
    window.removeEventListener('hashchange', this._onHashChange);
  }

  private _onHashChange = (): void => {
    this._syncViewFromHash();
  };

  private _syncViewFromHash(): void {
    const hash = window.location.hash.slice(1);
    if (hash && NAV_ITEMS.some(n => n.view === hash)) {
      this.currentView = hash as View;
    } else {
      this.currentView = 'home';
    }
  }

  private _navigate(view: View): void {
    window.location.hash = view;
  }

  override render() {
    return html`
      <nav>
        <span class="brand">Household Hub</span>
        ${NAV_ITEMS.map(n => html`
          <a
            ?data-active=${this.currentView === n.view}
            @click=${() => this._navigate(n.view)}
          >${n.label}</a>
        `)}
        <span class="spacer"></span>
        <div class="toolbar">
          <button title="Notifications" aria-label="Notifications">🔔</button>
          <button title="Toggle theme" aria-label="Toggle theme"
            @click=${this._toggleTheme}>${this._darkMode ? '☀️' : '🌙'}</button>
          <span class="user">Mark (Admin)</span>
        </div>
      </nav>
      <main>
        ${this._renderView()}
      </main>
    `;
  }

  private _toggleTheme(): void {
    this._darkMode = !this._darkMode;
    applyTheme(this._darkMode ? 'casehub-dark' : 'casehub-light');
  }

  private _renderView() {
    switch (this.currentView) {
      case 'home': return html`<home-view></home-view>`;
      case 'inbox': return html`<inbox-view></inbox-view>`;
      case 'people': return html`<people-view></people-view>`;
      case 'cases': return html`<cases-view></cases-view>`;
      case 'journal': return html`<journal-view></journal-view>`;
      default: return html`<div class="placeholder">Unknown view</div>`;
    }
  }
}
