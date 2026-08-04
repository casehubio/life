import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import { dockWorkbench, hostPanel } from '@casehubio/pages-ui/dist/dsl/builders.js';
import { loadSite } from '@casehubio/pages-runtime';
import { createLocalLayoutStore } from '@casehubio/pages-runtime/dist/layout-store.js';
import type { DockWorkbenchConfig } from '@casehubio/pages-ui/dist/dsl/builders.js';

@customElement('home-view')
export class HomeView extends LitElement {
  static override styles = css`
    :host {
      display: block;
      height: 100%;
    }
    #dock-container {
      height: 100%;
    }
  `;

  private _siteDispose?: () => void;

  override async firstUpdated(): Promise<void> {
    const container = this.renderRoot.querySelector('#dock-container') as HTMLElement;
    if (!container) return;

    const config: DockWorkbenchConfig = {
      storageKey: 'life-dashboard',
      centre: hostPanel('life-centre-content'),
      left: [
        {
          key: 'inbox',
          label: 'Inbox',
          icon: 'inbox',
          defaultOpen: true,
          content: hostPanel('life-inbox-dock'),
          minSize: 200,
        },
        {
          key: 'cases',
          label: 'Cases',
          icon: 'cases',
          content: hostPanel('life-cases-dock'),
          minSize: 200,
        },
        {
          key: 'calendar',
          label: 'Calendar',
          icon: 'calendar',
          content: hostPanel('life-calendar-mock'),
        },
      ],
      right: [
        {
          key: 'family',
          label: 'Family',
          icon: 'people',
          defaultOpen: true,
          content: hostPanel('life-family-summary'),
          minSize: 240,
        },
        {
          key: 'money',
          label: 'Money',
          icon: 'currency',
          content: hostPanel('life-money-mock'),
        },
        {
          key: 'comms',
          label: 'Comms',
          icon: 'chat',
          content: hostPanel('life-comms-mock'),
        },
      ],
    };

    const workbench = dockWorkbench(config);
    const site = await loadSite(container, workbench, {
      layoutStore: createLocalLayoutStore(),
      layoutKey: 'life-dashboard',
    });
    this._siteDispose = () => site.dispose();
  }

  override disconnectedCallback(): void {
    super.disconnectedCallback();
    this._siteDispose?.();
  }

  override render() {
    return html`<div id="dock-container"></div>`;
  }
}
