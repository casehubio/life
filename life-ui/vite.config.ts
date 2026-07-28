import { defineConfig } from 'vite';
import { resolve } from 'path';

const blocksUi = resolve(__dirname, '.casehub-packages/packages');
const pages = resolve(__dirname, '.casehub-packages/packages');

export default defineConfig({
  root: __dirname,
  resolve: {
    alias: [
      { find: '@casehubio/blocks-ui-core', replacement: resolve(blocksUi, 'blocks-ui-core') },
      { find: '@casehubio/blocks-ui-work-item-workbench', replacement: resolve(blocksUi, 'work-item-workbench') },
      { find: '@casehubio/blocks-ui-work-item-inbox', replacement: resolve(blocksUi, 'work-item-inbox') },
      { find: '@casehubio/blocks-ui-work-item-detail', replacement: resolve(blocksUi, 'work-item-detail') },
      { find: '@casehubio/blocks-ui-work-item-row', replacement: resolve(blocksUi, 'work-item-row') },
      { find: '@casehubio/blocks-ui-kpi-metric-row', replacement: resolve(blocksUi, 'kpi-metric-row') },
      { find: '@casehubio/blocks-ui-sla-indicator', replacement: resolve(blocksUi, 'sla-indicator') },
      { find: '@casehubio/blocks-ui-sla-breach-policy', replacement: resolve(blocksUi, 'sla-breach-policy') },
      { find: '@casehubio/blocks-ui-grouped-data-view', replacement: resolve(blocksUi, 'grouped-data-view') },
      { find: '@casehubio/blocks-ui-notification-inbox', replacement: resolve(blocksUi, 'notification-inbox') },
      { find: '@casehubio/blocks-ui-split-workbench', replacement: resolve(blocksUi, 'split-workbench') },
      { find: '@casehubio/blocks-ui-list-pane', replacement: resolve(blocksUi, 'list-pane') },
      { find: '@casehubio/blocks-ui-detail-pane', replacement: resolve(blocksUi, 'detail-pane') },
      { find: '@casehubio/pages-ui-tokens', replacement: resolve(pages, 'pages-ui-tokens') },
      { find: /^@casehubio\/pages-component\/dist\/(.*)/, replacement: resolve(pages, 'pages-component/dist/$1') },
      { find: '@casehubio/pages-component', replacement: resolve(pages, 'pages-component') },
      { find: /^@casehubio\/pages-data\/dist\/(.*)/, replacement: resolve(pages, 'pages-data/dist/$1') },
      { find: '@casehubio/pages-data', replacement: resolve(pages, 'pages-data') },
      { find: '@casehubio/pages-primitives', replacement: resolve(pages, 'pages-primitives') },
      { find: '@casehubio/pages-table', replacement: resolve(pages, 'pages-table') },
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
    },
  },
  server: {
    proxy: {
      '/life-cases': 'http://localhost:8080',
      '/life-tasks': 'http://localhost:8080',
      '/pending-actions': 'http://localhost:8080',
      '/external-actors': 'http://localhost:8080',
      '/analytics': 'http://localhost:8080',
      '/events': 'http://localhost:8080',
    },
    fs: {
      allow: ['..', '.casehub-packages'],
    },
  },
});
