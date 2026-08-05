import { SSEManager } from '@casehubio/pages-data/dist/sse/index.js';
import type { SSEEvent, SSEHandler } from '@casehubio/pages-data/dist/sse/index.js';
import type { ReactiveController, ReactiveControllerHost } from 'lit';

export interface LifeSseEvent {
  type: string;
  data: Record<string, unknown>;
  timestamp: string;
}

export interface LifeEventControllerOptions {
  types?: string[];
  onEvent?: (event: LifeSseEvent) => void;
  debounceMs?: number;
}

export const LifeEventTypes = {
  WORK_ITEM_CREATED: 'WORK_ITEM_CREATED',
  WORK_ITEM_UPDATED: 'WORK_ITEM_UPDATED',
  WORK_ITEM_COMPLETED: 'WORK_ITEM_COMPLETED',
  SLA_BREACH: 'SLA_BREACH',
  CASE_STARTED: 'CASE_STARTED',
  CASE_COMPLETED: 'CASE_COMPLETED',
  CASE_FAULTED: 'CASE_FAULTED',
} as const;

export const INBOX_EVENT_TYPES = [
  LifeEventTypes.WORK_ITEM_CREATED,
  LifeEventTypes.WORK_ITEM_UPDATED,
  LifeEventTypes.WORK_ITEM_COMPLETED,
  LifeEventTypes.SLA_BREACH,
];

export const CASE_EVENT_TYPES = [
  LifeEventTypes.CASE_STARTED,
  LifeEventTypes.CASE_COMPLETED,
  LifeEventTypes.CASE_FAULTED,
];

export const ALL_EVENT_TYPES = [...INBOX_EVENT_TYPES, ...CASE_EVENT_TYPES];

const SSE_URL = '/events/stream';
const manager = new SSEManager();

const DEFAULT_DEBOUNCE_MS = 300;

export class LifeEventController implements ReactiveController {
  private readonly _host: ReactiveControllerHost;
  private readonly _types: Set<string> | undefined;
  private readonly _onEvent: ((event: LifeSseEvent) => void) | undefined;
  private readonly _debounceMs: number;
  private readonly _handler: SSEHandler;
  private _latest: LifeSseEvent | undefined;
  private _unreadCount = 0;
  private _debounceTimer: ReturnType<typeof setTimeout> | undefined;

  constructor(host: ReactiveControllerHost, options?: LifeEventControllerOptions) {
    this._host = host;
    this._types = options?.types ? new Set(options.types) : undefined;
    this._onEvent = options?.onEvent;
    this._debounceMs = options?.debounceMs ?? DEFAULT_DEBOUNCE_MS;

    this._handler = (event: SSEEvent) => {
      const sseEvent = event.data as LifeSseEvent;
      if (!sseEvent.type) return;
      if (this._types && !this._types.has(sseEvent.type)) return;
      this._latest = sseEvent;
      this._unreadCount++;
      this._host.requestUpdate();
      if (this._onEvent) {
        clearTimeout(this._debounceTimer);
        this._debounceTimer = setTimeout(() => this._onEvent!(sseEvent), this._debounceMs);
      }
    };

    host.addController(this);
  }

  get latest(): LifeSseEvent | undefined {
    return this._latest;
  }

  get unreadCount(): number {
    return this._unreadCount;
  }

  get status(): 'connected' | 'reconnecting' | 'disconnected' {
    return manager.status(SSE_URL);
  }

  clearUnread(): void {
    this._unreadCount = 0;
    this._host.requestUpdate();
  }

  hostConnected(): void {
    manager.subscribe(SSE_URL, this._handler);
  }

  hostDisconnected(): void {
    manager.unsubscribe(SSE_URL, this._handler);
    clearTimeout(this._debounceTimer);
  }
}
