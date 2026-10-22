import { defineConfig } from 'vite';
import { resolve } from 'path';

const blocksUi = resolve(__dirname, '.casehub-packages/packages');
const pages = resolve(__dirname, '.casehub-packages/packages');

export default defineConfig({
  root: __dirname,
  resolve: {
    alias: [
      { find: /^@casehubio\/mosaic-core\/(.*)/, replacement: resolve(blocksUi, 'mosaic-core/dist/$1') },
      { find: '@casehubio/mosaic-core', replacement: resolve(blocksUi, 'mosaic-core') },
      { find: '@casehubio/mosaic-work-item-workbench', replacement: resolve(blocksUi, 'work-item-workbench') },
      { find: '@casehubio/mosaic-work-item-inbox', replacement: resolve(blocksUi, 'work-item-inbox') },
      { find: '@casehubio/mosaic-work-item-detail', replacement: resolve(blocksUi, 'work-item-detail') },
      { find: '@casehubio/mosaic-work-item-row', replacement: resolve(blocksUi, 'work-item-row') },
      { find: '@casehubio/mosaic-kpi-metric-row', replacement: resolve(blocksUi, 'kpi-metric-row') },
      { find: '@casehubio/mosaic-sla-indicator', replacement: resolve(blocksUi, 'sla-indicator') },
      { find: '@casehubio/mosaic-sla-breach-policy', replacement: resolve(blocksUi, 'sla-breach-policy') },
      { find: '@casehubio/mosaic-grouped-data-view', replacement: resolve(blocksUi, 'grouped-data-view') },
      { find: '@casehubio/mosaic-notification-inbox', replacement: resolve(blocksUi, 'notification-inbox') },
      { find: '@casehubio/mosaic-split-workbench', replacement: resolve(blocksUi, 'split-workbench') },
      { find: '@casehubio/mosaic-list-pane', replacement: resolve(blocksUi, 'list-pane') },
      { find: '@casehubio/mosaic-detail-pane', replacement: resolve(blocksUi, 'detail-pane') },
      { find: '@casehubio/mosaic-audit-trail-viewer', replacement: resolve(blocksUi, 'audit-trail-viewer') },
      { find: '@casehubio/mosaic-routing-rationale', replacement: resolve(blocksUi, 'routing-rationale') },
      { find: '@casehubio/mosaic-similarity-panel', replacement: resolve(blocksUi, 'similarity-panel') },
      { find: '@casehubio/mosaic-channel-activity', replacement: resolve(blocksUi, 'channel-activity') },
      { find: '@casehubio/mosaic-commitment-viz', replacement: resolve(blocksUi, 'commitment-viz') },
      { find: '@casehubio/mosaic-approval-gate', replacement: resolve(blocksUi, 'approval-gate') },
      { find: '@casehubio/mosaic-trust-score-panel', replacement: resolve(blocksUi, 'trust-score-panel') },
      { find: '@casehubio/mosaic-compliance-summary', replacement: resolve(blocksUi, 'compliance-summary') },
      { find: '@casehubio/mosaic-blocks-timeline', replacement: resolve(blocksUi, 'agency-timeline') },
      { find: /^@casehubio\/pages-ui-tokens\/dist\/(.*)/, replacement: resolve(pages, 'pages-ui-tokens/dist/$1') },
      { find: '@casehubio/pages-ui-tokens', replacement: resolve(pages, 'pages-ui-tokens') },
      { find: /^@casehubio\/pages-ui\/dist\/(.*)/, replacement: resolve(pages, 'pages-ui/dist/$1') },
      { find: '@casehubio/pages-ui', replacement: resolve(pages, 'pages-ui') },
      { find: /^@casehubio\/pages-runtime\/dist\/(.*)/, replacement: resolve(pages, 'pages-runtime/dist/$1') },
      { find: '@casehubio/pages-runtime', replacement: resolve(pages, 'pages-runtime') },
      { find: '@casehubio/pages-viz', replacement: resolve(pages, 'pages-viz') },
      { find: /^@casehubio\/pages-ui-components\/(.*)/, replacement: resolve(pages, 'pages-ui-components/dist/$1') },
      { find: '@casehubio/pages-ui-components', replacement: resolve(pages, 'pages-ui-components') },
      { find: /^@casehubio\/pages-component\/dist\/(.*)/, replacement: resolve(pages, 'pages-component/dist/$1') },
      { find: '@casehubio/pages-component', replacement: resolve(pages, 'pages-component') },
      { find: /^@casehubio\/pages-data\/dist\/(.*)/, replacement: resolve(pages, 'pages-data/dist/$1') },
      { find: '@casehubio/pages-data', replacement: resolve(pages, 'pages-data') },
      { find: '@casehubio/pages-primitives', replacement: resolve(pages, 'pages-primitives') },
      { find: '@casehubio/pages-table', replacement: resolve(pages, 'pages-table') },
      { find: /^@casehubio\/pages-ui\/dist\/(.*)/, replacement: resolve(pages, 'pages-ui/dist/$1') },
      { find: '@casehubio/pages-ui', replacement: resolve(pages, 'pages-ui') },
      { find: /^@casehubio\/pages-runtime\/dist\/(.*)/, replacement: resolve(pages, 'pages-runtime/dist/$1') },
      { find: '@casehubio/pages-runtime', replacement: resolve(pages, 'pages-runtime') },
      { find: '@casehubio/pages-viz', replacement: resolve(pages, 'pages-viz') },
      { find: /^@casehubio\/pages-ui-components\/(.*)/, replacement: resolve(pages, 'pages-ui-components/dist/$1') },
      { find: '@casehubio/pages-ui-components', replacement: resolve(pages, 'pages-ui-components') },
    ],
  },
  esbuild: {
    target: 'es2022',
    tsconfigRaw: {
      compilerOptions: {
        experimentalDecorators: true,
        useDefineForClassFields: false,
      },
    },
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      input: resolve(__dirname, 'index.html'),
      external: [
        'react', 'react-dom', 'react-markdown',
        '@xterm/xterm', '@xterm/addon-fit',
        'heatmap.js', 'emoji-picker-element',
        '@openworkflowspec/sdk',
      ],
    },
  },
  server: {
    proxy: {
      '/dashboard': 'http://127.0.0.1:8080',
      '/life-cases': 'http://127.0.0.1:8080',
      '/life-tasks': 'http://127.0.0.1:8080',
      '/pending-actions': 'http://127.0.0.1:8080',
      '/external-actors': 'http://127.0.0.1:8080',
      '/analytics': 'http://127.0.0.1:8080',
      '/api/v1/ledger': 'http://127.0.0.1:8080',
      '/events': 'http://127.0.0.1:8080',
    },
    fs: {
      allow: ['..', '.casehub-packages'],
    },
  },
});
