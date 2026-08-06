import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '../panels/register.js';

interface DockPanel {
  key: string;
  label: string;
  tag: string;
}

const LEFT_PANELS: DockPanel[] = [
  { key: 'inbox', label: 'Inbox', tag: 'life-inbox-dock' },
  { key: 'cases', label: 'Cases', tag: 'life-cases-dock' },
  { key: 'calendar', label: 'Calendar', tag: 'life-calendar-mock' },
];

const RIGHT_PANELS: DockPanel[] = [
  { key: 'family', label: 'Family', tag: 'life-family-summary' },
  { key: 'money', label: 'Money', tag: 'life-money-mock' },
  { key: 'comms', label: 'Comms', tag: 'life-comms-mock' },
];

@customElement('home-view')
export class HomeView extends LitElement {
  static override styles = css`
    :host { display: block; height: 100%; }
    .layout {
      display: grid;
      height: 100%;
      grid-template-columns: auto 1fr auto;
      gap: 1px;
      background: var(--pages-neutral-4, #d4d4d4);
    }
    .dock-bar {
      display: flex;
      flex-direction: column;
      background: var(--pages-neutral-2, #f5f5f5);
      width: 32px;
    }
    .dock-tab {
      writing-mode: vertical-lr;
      text-orientation: mixed;
      padding: var(--pages-space-3, 12px) var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      font-weight: 500;
      color: var(--pages-neutral-9, #525252);
      cursor: pointer;
      border: none; background: none;
      transition: background var(--pages-duration-fast, 120ms);
      text-align: center;
    }
    .dock-tab:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .dock-tab[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3);
    }
    .dock-panel {
      width: 280px;
      background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto;
      border-right: 1px solid var(--pages-neutral-4, #d4d4d4);
    }
    .dock-panel.right-panel {
      border-right: none;
      border-left: 1px solid var(--pages-neutral-4, #d4d4d4);
    }
    .left-zone, .right-zone {
      display: flex;
    }
    .right-zone { flex-direction: row-reverse; }
    .centre {
      background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto;
    }
  `;

  @state() private _leftActive: string | null = 'inbox';
  @state() private _rightActive: string | null = 'family';

  private _toggleLeft(key: string): void {
    this._leftActive = this._leftActive === key ? null : key;
  }

  private _toggleRight(key: string): void {
    this._rightActive = this._rightActive === key ? null : key;
  }

  private _renderPanel(panel: DockPanel, side: 'left' | 'right') {
    const el = document.createElement(panel.tag);
    return html`<div class="dock-panel ${side === 'right' ? 'right-panel' : ''}">${el}</div>`;
  }

  override render() {
    const leftPanel = LEFT_PANELS.find(p => p.key === this._leftActive);
    const rightPanel = RIGHT_PANELS.find(p => p.key === this._rightActive);

    return html`
      <div class="layout">
        <div class="left-zone">
          <div class="dock-bar">
            ${LEFT_PANELS.map(p => html`
              <button class="dock-tab" ?data-active=${this._leftActive === p.key}
                      @click=${() => this._toggleLeft(p.key)}>${p.label}</button>
            `)}
          </div>
          ${leftPanel ? this._renderPanel(leftPanel, 'left') : nothing}
        </div>
        <div class="centre">
          <life-centre-content></life-centre-content>
        </div>
        <div class="right-zone">
          <div class="dock-bar">
            ${RIGHT_PANELS.map(p => html`
              <button class="dock-tab" ?data-active=${this._rightActive === p.key}
                      @click=${() => this._toggleRight(p.key)}>${p.label}</button>
            `)}
          </div>
          ${rightPanel ? this._renderPanel(rightPanel, 'right') : nothing}
        </div>
      </div>
    `;
  }
}
