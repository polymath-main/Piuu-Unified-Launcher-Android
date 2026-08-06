const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const archiver = require('archiver');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 850,
    minWidth: 1000,
    minHeight: 700,
    title: 'Piuu Extension Studio — Marketplace Builder',
    webPreferences: {
      preload: path.join(__dirname, 'src', 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// IPC Handler: Compile .piuu Extension Archive Bundle
ipcMain.handle('compile-piuu-bundle', async (event, bundleData) => {
  try {
    const { manifest, files, outputPath } = bundleData;
    const savePath = outputPath || path.join(app.getPath('downloads'), `${manifest.id}.piuu`);

    const output = fs.createWriteStream(savePath);
    const archive = archiver('zip', { zlib: { level: 9 } });

    return new Promise((resolve, reject) => {
      output.on('close', () => {
        const hash = crypto.createHash('sha256').update(fs.readFileSync(savePath)).digest('hex');
        resolve({ success: true, path: savePath, hash, sizeBytes: archive.pointer() });
      });

      archive.on('error', (err) => reject({ success: false, error: err.message }));

      archive.pipe(output);

      // Write plugin.json manifest
      archive.append(JSON.stringify(manifest, null, 2), { name: 'plugin.json' });

      // Write extra asset files if present
      if (files && Array.isArray(files)) {
        for (const f of files) {
          archive.append(f.content, { name: f.name });
        }
      }

      archive.finalize();
    });
  } catch (err) {
    return { success: false, error: err.message };
  }
});

// IPC Handler: Verify Manifest JSON
ipcMain.handle('validate-manifest-json', async (event, jsonString) => {
  try {
    const parsed = JSON.parse(jsonString);
    if (!parsed.id || !parsed.name || !parsed.category) {
      return { valid: false, error: "Missing required fields: 'id', 'name', or 'category'" };
    }
    return { valid: true, manifest: parsed };
  } catch (e) {
    return { valid: false, error: "Invalid JSON syntax: " + e.message };
  }
});
