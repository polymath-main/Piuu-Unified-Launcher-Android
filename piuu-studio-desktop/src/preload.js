const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('piuuStudio', {
  compilePiuuBundle: (bundleData) => ipcRenderer.invoke('compile-piuu-bundle', bundleData),
  validateManifestJson: (jsonString) => ipcRenderer.invoke('validate-manifest-json', jsonString),
  platform: process.platform,
  version: '1.0.0'
});
