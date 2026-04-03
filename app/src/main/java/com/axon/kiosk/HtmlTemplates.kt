package com.axon.kiosk

object HtmlTemplates {

    const val LIVE_VIEW_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>LIBERA KIOSK - Live View</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#1a1a2e;min-height:100vh;color:#fff;display:flex;flex-direction:column}.header{background:rgba(255,255,255,0.05);padding:15px 20px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(255,255,255,0.1)}.header h1{font-size:1.3em;background:linear-gradient(90deg,#00d4ff,#7b2cbf);-webkit-background-clip:text;-webkit-text-fill-color:transparent}.btn{padding:8px 16px;border:none;border-radius:8px;font-size:13px;cursor:pointer}.btn-back{background:transparent;border:1px solid rgba(255,255,255,0.3);color:#fff}.main{flex:1;display:flex;justify-content:center;align-items:center;padding:20px}.screen-container{background:#000;border-radius:12px;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,0.5)}.screen-frame{border:3px solid #333;border-radius:12px;overflow:hidden}#liveScreen{display:block;max-width:90vw;max-height:75vh}.loading{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);text-align:center}.spinner{width:40px;height:40px;border:3px solid rgba(255,255,255,0.1);border-top-color:#00d4ff;border-radius:50%;animation:spin 1s linear infinite;margin:0 auto 15px}@keyframes spin{to{transform:rotate(360deg)}}.status{display:flex;align-items:center;gap:8px}.status-dot{width:8px;height:8px;border-radius:50%;background:#4caf50;animation:pulse 2s infinite}.status-dot.error{background:#f44336;animation:none}@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.5}}.controls{background:rgba(255,255,255,0.05);padding:15px;display:flex;justify-content:center;gap:20px;flex-wrap:wrap}.control-group{display:flex;align-items:center;gap:10px}select{padding:8px 12px;border-radius:6px;background:rgba(255,255,255,0.1);border:1px solid rgba(255,255,255,0.2);color:#fff}
</style>
</head>
<body>
<header class="header"><h1>📺 Live View</h1><div style="display:flex;gap:15px;align-items:center"><div class="status"><span class="status-dot" id="statusDot"></span><span id="statusText">Connecting...</span></div><button class="btn btn-back" onclick="location.href='/admin?token='+token">← Back</button></div></header>
<main class="main"><div class="screen-container"><div class="screen-frame"><img id="liveScreen" style="display:none"><div class="loading" id="loading"><div class="spinner"></div><div>Connecting...</div></div></div></div></main>
<div class="controls"><div class="control-group"><span>Refresh:</span><select id="refreshRate" onchange="updateRate()"><option value="500">0.5s</option><option value="1000" selected>1s</option><option value="2000">2s</option></select></div><span id="fpsDisplay">FPS: --</span></div>
<script>
const token=new URLSearchParams(location.search).get('token')||localStorage.getItem('token');if(!token)location.href='/';
let interval=1000,timer,frames=0,lastFps=Date.now(),errors=0;
const img=document.getElementById('liveScreen'),loading=document.getElementById('loading'),dot=document.getElementById('statusDot'),status=document.getElementById('statusText');
function fetch_(){fetch('/api/screenshot?token='+token+'&t='+Date.now()).then(r=>{if(!r.ok)throw Error();return r.blob();}).then(b=>{const u=URL.createObjectURL(b);img.onload=()=>{URL.revokeObjectURL(u);loading.style.display='none';img.style.display='block';dot.className='status-dot';status.textContent='Connected';errors=0;frames++;if(Date.now()-lastFps>=1000){document.getElementById('fpsDisplay').textContent='FPS: '+frames;frames=0;lastFps=Date.now();}};img.src=u;}).catch(()=>{if(++errors>3){dot.className='status-dot error';status.textContent='Disconnected';}});timer=setTimeout(fetch_,interval);}
function updateRate(){interval=parseInt(document.getElementById('refreshRate').value);}
fetch_();
</script>
</body>
</html>"""

    const val LOGIN_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>LIBERA KIOSK - Login</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:linear-gradient(135deg,#0F172A,#1e293b,#0f3460);min-height:100vh;display:flex;align-items:center;justify-content:center;color:#fff}.container{background:rgba(255,255,255,0.1);backdrop-filter:blur(10px);border-radius:20px;padding:40px;width:100%;max-width:420px;border:1px solid rgba(255,255,255,0.1)}.logo{text-align:center;margin-bottom:30px}.logo svg{max-width:280px;height:auto}.form-group{margin-bottom:25px}.form-group input{width:100%;padding:15px;border:2px solid rgba(255,255,255,0.1);border-radius:12px;background:rgba(255,255,255,0.05);color:#fff;font-size:16px}.form-group input:focus{outline:none;border-color:#2563EB}.btn{width:100%;padding:15px;border:none;border-radius:12px;background:linear-gradient(90deg,#2563EB,#7b2cbf);color:#fff;font-size:16px;font-weight:600;cursor:pointer}.error{background:rgba(255,82,82,0.2);color:#ff5252;padding:12px;border-radius:8px;margin-bottom:20px;display:none}.error.show{display:block}.version{text-align:center;margin-top:20px;color:rgba(255,255,255,0.4);font-size:12px}
</style>
</head>
<body>
<div class="container">
<div class="logo">
<svg width="280" height="75" viewBox="0 0 450 120" xmlns="http://www.w3.org/2000/svg">
<rect width="450" height="120" fill="#0F172A" rx="10"/>
<g transform="translate(25, 25)"><path d="M2,5 Q2,0 7,0 H63 Q68,0 68,5 V40 Q68,45 63,45 H7 Q2,45 2,40 Z" fill="none" stroke="#2563EB" stroke-width="3.5"/><path d="M25,45 L22,55 H48 L45,45" fill="#2563EB"/><rect x="30" y="22" width="10" height="8" rx="1.5" fill="#F59E0B"/><path d="M32,22 V19 A3,3 0 0 1 38,19 V22" fill="none" stroke="#F59E0B" stroke-width="2"/><circle cx="12" cy="12" r="2" fill="#2563EB" opacity="0.6"/><circle cx="12" cy="22" r="2" fill="#2563EB" opacity="0.8"/><circle cx="12" cy="32" r="2" fill="#2563EB"/></g>
<g transform="translate(110, 58)"><text font-family="Segoe UI, Roboto, Arial, sans-serif" font-weight="800" font-size="38" fill="#FFFFFF" letter-spacing="1">LIBERA</text><text x="135" font-family="Segoe UI, Roboto, Arial, sans-serif" font-weight="300" font-size="38" fill="#2563EB">KIOSK</text><text x="2" y="28" font-family="Segoe UI, Roboto, Arial, sans-serif" font-weight="500" font-size="12" fill="#94A3B8" letter-spacing="3">ANDROID KIOSK SOLUTION</text></g>
</svg>
<p style="color:rgba(255,255,255,0.6);margin-top:15px">Web Administration</p>
</div>
<div class="error" id="error"></div>
<form id="loginForm">
<div class="form-group"><input type="password" id="password" placeholder="Enter password" required autofocus></div>
<button type="submit" class="btn">Login</button>
</form>
<p class="version">Default: 2580</p>
</div>
<script>
document.getElementById('loginForm').onsubmit=async e=>{e.preventDefault();const err=document.getElementById('error');err.classList.remove('show');try{const r=await fetch('/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({password:document.getElementById('password').value})});const d=await r.json();if(d.token){localStorage.setItem('token',d.token);location.href='/admin?token='+d.token;}else{err.textContent=d.error||'Error';err.classList.add('show');}}catch(ex){err.textContent='Connection error';err.classList.add('show');}};
</script>
</body>
</html>"""

    const val ADMIN_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>LIBERA KIOSK Admin</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:linear-gradient(135deg,#1a1a2e,#16213e,#0f3460);min-height:100vh;color:#fff}.header{background:rgba(255,255,255,0.05);padding:15px 25px;display:flex;justify-content:space-between;align-items:center;backdrop-filter:blur(10px);border-bottom:1px solid rgba(255,255,255,0.1);position:sticky;top:0;z-index:100}.header h1{font-size:1.4em;background:linear-gradient(90deg,#00d4ff,#7b2cbf);-webkit-background-clip:text;-webkit-text-fill-color:transparent}.logout-btn{padding:8px 16px;background:transparent;border:1px solid rgba(255,255,255,0.3);border-radius:8px;color:#fff;cursor:pointer}.container{max-width:1400px;margin:0 auto;padding:20px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(380px,1fr));gap:20px}.card{background:rgba(255,255,255,0.05);border-radius:16px;border:1px solid rgba(255,255,255,0.1);overflow:hidden}.card-header{padding:15px 20px;background:rgba(255,255,255,0.05);display:flex;align-items:center;gap:10px;border-bottom:1px solid rgba(255,255,255,0.1)}.card-header h2{font-size:1em}.card-body{padding:20px}.form-group{margin-bottom:15px}.form-group label{display:block;font-size:13px;color:rgba(255,255,255,0.7);margin-bottom:6px}.form-group input,.form-group select{width:100%;padding:12px 15px;border:1px solid rgba(255,255,255,0.2);border-radius:10px;background:rgba(255,255,255,0.05);color:#fff;font-size:14px}.form-group input:focus,.form-group select:focus{outline:none;border-color:#00d4ff}.toggle-group{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.05)}.toggle-label{font-size:14px}.toggle-label small{display:block;font-size:11px;color:rgba(255,255,255,0.5)}.toggle{position:relative;width:46px;height:24px}.toggle input{opacity:0;width:0;height:0}.toggle-slider{position:absolute;cursor:pointer;inset:0;background:rgba(255,255,255,0.2);transition:0.3s;border-radius:24px}.toggle-slider:before{position:absolute;content:"";height:18px;width:18px;left:3px;bottom:3px;background:#fff;transition:0.3s;border-radius:50%}.toggle input:checked+.toggle-slider{background:linear-gradient(90deg,#00d4ff,#7b2cbf)}.toggle input:checked+.toggle-slider:before{transform:translateX(22px)}.range-group{display:flex;align-items:center;gap:15px}.range-group input[type="range"]{flex:1;height:6px;-webkit-appearance:none;background:rgba(255,255,255,0.2);border-radius:3px}.range-group input[type="range"]::-webkit-slider-thumb{-webkit-appearance:none;width:18px;height:18px;background:linear-gradient(135deg,#00d4ff,#7b2cbf);border-radius:50%;cursor:pointer}.range-value{min-width:45px;text-align:right;font-size:14px}.btn{padding:12px 24px;border:none;border-radius:10px;cursor:pointer;font-weight:600;font-size:14px;transition:all 0.2s}.btn-primary{background:linear-gradient(90deg,#00d4ff,#7b2cbf);color:#fff}.btn-warning{background:rgba(255,152,0,0.8);color:#fff}.btn-danger{background:rgba(244,67,54,0.8);color:#fff}.btn-success{background:rgba(76,175,80,0.8);color:#fff}.btn:hover{transform:translateY(-1px)}.btn:disabled{opacity:0.5}.btn-group{display:flex;gap:10px;flex-wrap:wrap}.status-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.status-item{text-align:center;padding:12px;background:rgba(255,255,255,0.05);border-radius:10px}.status-item .value{font-size:1.4em;font-weight:700;color:#00d4ff}.status-item .label{font-size:11px;color:rgba(255,255,255,0.6);margin-top:4px}.save-container{margin-top:20px;text-align:center;padding:20px}.save-container .btn{min-width:250px;padding:18px 50px;font-size:18px}.toast{position:fixed;bottom:30px;left:50%;transform:translateX(-50%);padding:15px 30px;border-radius:10px;font-weight:500;opacity:0;transition:opacity 0.3s;z-index:1000}.toast.success{background:rgba(76,175,80,0.95)}.toast.error{background:rgba(244,67,54,0.95)}.toast.show{opacity:1}.footer{background:rgba(0,0,0,0.3);padding:15px 25px;display:flex;justify-content:space-between;align-items:center;font-size:12px;color:rgba(255,255,255,0.5);margin-top:20px;border-radius:10px}.time-inputs{display:flex;gap:10px;align-items:center}.time-inputs input{width:100px}.app-list{max-height:200px;overflow-y:auto;border:1px solid rgba(255,255,255,0.1);border-radius:8px;margin-top:10px}.app-item{padding:10px 15px;cursor:pointer;border-bottom:1px solid rgba(255,255,255,0.05);display:flex;justify-content:space-between;align-items:center}.app-item:hover{background:rgba(255,255,255,0.1)}.app-item.selected{background:rgba(0,212,255,0.2)}.upload-area{border:2px dashed rgba(255,255,255,0.3);border-radius:10px;padding:30px;text-align:center;cursor:pointer;transition:all 0.3s}.upload-area:hover{border-color:#00d4ff;background:rgba(0,212,255,0.1)}.upload-area.dragover{border-color:#00d4ff;background:rgba(0,212,255,0.2)}
</style>
</head>
<body>
<header class="header"><div style="display:flex;align-items:center;gap:15px"><svg width="140" height="38" viewBox="0 0 450 120" xmlns="http://www.w3.org/2000/svg"><rect width="450" height="120" fill="transparent" rx="10"/><g transform="translate(25, 25)"><path d="M2,5 Q2,0 7,0 H63 Q68,0 68,5 V40 Q68,45 63,45 H7 Q2,45 2,40 Z" fill="none" stroke="#2563EB" stroke-width="3.5"/><path d="M25,45 L22,55 H48 L45,45" fill="#2563EB"/><rect x="30" y="22" width="10" height="8" rx="1.5" fill="#F59E0B"/><path d="M32,22 V19 A3,3 0 0 1 38,19 V22" fill="none" stroke="#F59E0B" stroke-width="2"/><circle cx="12" cy="12" r="2" fill="#2563EB" opacity="0.6"/><circle cx="12" cy="22" r="2" fill="#2563EB" opacity="0.8"/><circle cx="12" cy="32" r="2" fill="#2563EB"/></g><g transform="translate(110, 58)"><text font-family="Segoe UI,Roboto,Arial,sans-serif" font-weight="800" font-size="38" fill="#FFFFFF" letter-spacing="1">LIBERA</text><text x="135" font-family="Segoe UI,Roboto,Arial,sans-serif" font-weight="300" font-size="38" fill="#2563EB">KIOSK</text></g></svg></div><div style="display:flex;gap:15px;align-items:center"><span style="font-size:12px;color:rgba(255,255,255,0.5)" id="deviceIp"></span><button class="logout-btn" onclick="logout()">Logout</button></div></header>
<div class="container">
<div class="grid">

<div class="card"><div class="card-header"><span>📊</span><h2>System Status</h2></div><div class="card-body"><div class="status-grid"><div class="status-item"><div class="value" id="batteryLevel">--%</div><div class="label">🔋 Battery</div></div><div class="status-item"><div class="value" id="wifiStatus">--</div><div class="label">📶 WiFi</div></div><div class="status-item"><div class="value" id="deviceOwner">--</div><div class="label">🔐 Device Owner</div></div><div class="status-item"><div class="value" id="uptime">--</div><div class="label">⏱️ Uptime</div></div></div><div class="btn-group" style="margin-top:15px"><button class="btn btn-primary" onclick="openLiveView()">📺 Live</button><button class="btn btn-warning" onclick="reloadPage()">🔄 Reload</button><button class="btn btn-danger" onclick="rebootDevice()">⚡ Reboot</button></div></div></div>

<div class="card"><div class="card-header"><span>🌐</span><h2>Content Mode</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Use External App<small>Show app instead of URL</small></div><label class="toggle"><input type="checkbox" id="externalAppMode" onchange="toggleContentMode()"><span class="toggle-slider"></span></label></div><div id="urlSection"><div class="form-group"><label>Kiosk URL</label><input type="url" id="kioskUrl" placeholder="https://example.com"></div><div class="toggle-group"><div class="toggle-label">Desktop Mode<small>Use desktop user agent</small></div><label class="toggle"><input type="checkbox" id="desktopMode"><span class="toggle-slider"></span></label></div></div><div id="appSection" style="display:none"><div class="form-group"><label>Selected App</label><input type="text" id="externalAppPackage" placeholder="com.example.app" readonly><button class="btn btn-primary" onclick="loadApps()" style="margin-top:10px;width:100%">📱 Select App</button></div><div id="appListContainer" style="display:none"><input type="text" id="appSearch" placeholder="Search apps..." style="margin-bottom:10px"><div class="app-list" id="appList"></div></div><div class="btn-group" style="margin-top:15px"><button class="btn btn-success" onclick="startApp()">▶️ Start</button><button class="btn btn-warning" onclick="restartApp()">🔄 Restart</button><button class="btn btn-danger" onclick="stopApp()">⏹️ Stop</button></div></div><div class="toggle-group"><div class="toggle-label">Auto Reload on Error</div><label class="toggle"><input type="checkbox" id="autoReload"><span class="toggle-slider"></span></label></div><div class="toggle-group"><div class="toggle-label">Auto Reload on Inactivity</div><label class="toggle"><input type="checkbox" id="autoReloadEnabled"><span class="toggle-slider"></span></label></div><div class="form-group"><label>Inactivity Timeout (min)</label><input type="number" id="autoReloadMinutes" min="0" max="1440" value="0"></div></div></div>

<div class="card"><div class="card-header"><span>📦</span><h2>App Management</h2></div><div class="card-body"><div class="upload-area" id="uploadArea" onclick="document.getElementById('apkFile').click()"><input type="file" id="apkFile" accept=".apk" style="display:none" onchange="uploadApk()"><p>📁 Click or drag APK file here</p><small>Upload and install APK on device</small></div><div id="uploadStatus" style="margin-top:10px;text-align:center"></div><hr style="border-color:rgba(255,255,255,0.1);margin:15px 0"><div class="form-group"><label>Installed Apps Management</label><button class="btn btn-primary" onclick="loadInstalledApps()" style="width:100%;margin-top:10px">📋 Load Installed Apps</button></div><div id="installedAppsContainer" style="display:none;margin-top:15px"><input type="text" id="installedAppSearch" placeholder="Search installed apps..." style="margin-bottom:10px"><div class="app-list" id="installedAppList" style="max-height:200px"></div><div class="btn-group" style="margin-top:10px"><button class="btn btn-warning" onclick="updateSelectedApp()">🔄 Update</button><button class="btn btn-danger" onclick="uninstallSelectedApp()">🗑️ Uninstall</button></div></div></div></div>

<div class="card"><div class="card-header"><span>🔐</span><h2>Security</h2></div><div class="card-body"><div class="form-group"><label>PIN Code (App)</label><input type="password" id="pinCode" placeholder="4-6 digits" maxlength="6"></div><div class="form-group"><label>Max PIN Attempts</label><input type="number" id="maxPinAttempts" min="1" max="100" value="5"></div><div class="form-group"><label>Web Password</label><input type="password" id="webPassword" placeholder="Leave empty to keep"></div><div class="toggle-group"><div class="toggle-label">Pin App to Screen</div><label class="toggle"><input type="checkbox" id="pinAppToScreen"><span class="toggle-slider"></span></label></div></div></div>

<div class="card"><div class="card-header"><span>🖥️</span><h2>Display</h2></div><div class="card-body"><div class="form-group"><label>Brightness (Live)</label><div class="range-group"><input type="range" id="brightness" min="0" max="100" value="100"><span class="range-value" id="brightnessValue">100%</span></div></div><div class="toggle-group"><div class="toggle-label">Show Status Bar</div><label class="toggle"><input type="checkbox" id="showStatusBar"><span class="toggle-slider"></span></label></div><div class="toggle-group"><div class="toggle-label">Show Nav Buttons</div><label class="toggle"><input type="checkbox" id="showNavButtons"><span class="toggle-slider"></span></label></div></div></div>

<div class="card"><div class="card-header"><span>🌙</span><h2>Screensaver</h2></div><div class="card-body"><div class="form-group"><label>Timeout (min, 0=off)</label><input type="number" id="screensaverTimeout" min="0" max="60" value="0"></div><div class="form-group"><label>Screensaver Type</label><select id="screensaverType"><option value="black">Black Screen (dim)</option><option value="url">Load URL</option></select></div><div class="form-group"><label>Screensaver URL</label><input type="url" id="screensaverUrl" placeholder="https://..."></div></div></div>

<div class="card"><div class="card-header"><span>💡</span><h2>Screen Schedule</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Screen Always On</div><label class="toggle"><input type="checkbox" id="screenAlwaysOn"><span class="toggle-slider"></span></label></div><div class="toggle-group"><div class="toggle-label">Screen Off Schedule</div><label class="toggle"><input type="checkbox" id="screenOffEnabled"><span class="toggle-slider"></span></label></div><div class="form-group"><label>Time Format</label><select id="screenOffUse24h" onchange="updateTimeFormat()"><option value="true">24h (Europe)</option><option value="false">12h AM/PM (US)</option></select></div><div class="form-group"><label>Screen Off Time</label><div class="time-inputs"><input type="time" id="screenOffStart" value="22:00"><span>to</span><input type="time" id="screenOffEnd" value="07:00"></div></div></div></div>

<div class="card"><div class="card-header"><span>🔄</span><h2>Daily Reboot</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Enable Daily Reboot</div><label class="toggle"><input type="checkbox" id="dailyRebootEnabled"><span class="toggle-slider"></span></label></div><div class="form-group"><label>Reboot Time</label><input type="time" id="dailyRebootTime" value="03:00"></div><div class="toggle-group"><div class="toggle-label">24h Format</div><label class="toggle"><input type="checkbox" id="use24hFormat" checked><span class="toggle-slider"></span></label></div></div></div>

<div class="card"><div class="card-header"><span>🔒</span><h2>Kiosk Mode</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Enable Kiosk Mode<small>Full lockdown (Device Owner required)</small></div><label class="toggle"><input type="checkbox" id="kioskMode"><span class="toggle-slider"></span></label></div><div class="form-group"><label>Taps to Exit (default 7)</label><input type="number" id="kioskExitTaps" min="3" max="20" value="7"></div></div></div>

<div class="card"><div class="card-header"><span>🔐</span><h2>HTTPS Certificate</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Enable HTTPS<small>Secure admin connection</small></div><label class="toggle"><input type="checkbox" id="httpsEnabled" onchange="toggleHttps()"><span class="toggle-slider"></span></label></div><div class="form-group"><label>Certificate Status</label><pre id="certInfo" style="background:rgba(0,0,0,0.2);padding:10px;border-radius:8px;font-size:11px;white-space:pre-wrap;max-height:100px;overflow:auto">Loading...</pre></div><div class="btn-group"><button class="btn btn-primary" onclick="generateCert()">🔑 Generate New</button><button class="btn btn-danger" onclick="deleteCert()">🗑️ Delete</button></div><small style="display:block;margin-top:10px;color:rgba(255,255,255,0.5)">Certificate: Libera Kiosk Solutions</small></div></div>

<div class="card"><div class="card-header"><span>⚙️</span><h2>System</h2></div><div class="card-body"><div class="toggle-group"><div class="toggle-label">Auto Launch on Boot</div><label class="toggle"><input type="checkbox" id="autoLaunch"><span class="toggle-slider"></span></label></div><div class="toggle-group"><div class="toggle-label">Test Mode<small>Back button enabled</small></div><label class="toggle"><input type="checkbox" id="testMode"><span class="toggle-slider"></span></label></div></div></div>

</div>

<div class="footer"><div><span>📍 Device IP: </span><strong id="footerIp">--</strong></div><div><span>🌐 Admin URL: </span><strong id="footerUrl">--</strong></div></div>

<div class="save-container"><button class="btn btn-primary" onclick="saveSettings()" id="saveBtn">💾 Save Settings</button></div>
</div>
<div class="toast" id="toast"></div>
<script>
const token=new URLSearchParams(location.search).get('token')||localStorage.getItem('token');
if(!token)location.href='/';
const headers={'Content-Type':'application/json','Authorization':'Bearer '+token};
let allApps=[];

function toggleContentMode(){
const isApp=document.getElementById('externalAppMode').checked;
document.getElementById('urlSection').style.display=isApp?'none':'block';
document.getElementById('appSection').style.display=isApp?'block':'none';
}

async function loadApps(){
try{
showToast('Loading apps...','success');
const r=await fetch('/api/apps',{headers});
const d=await r.json();
allApps=d.apps||[];
renderApps(allApps);
document.getElementById('appListContainer').style.display='block';
}catch(e){showToast('Failed to load apps','error');}
}

function renderApps(apps){
const list=document.getElementById('appList');
const current=document.getElementById('externalAppPackage').value;
list.innerHTML=apps.map(function(a){return '<div class="app-item '+(a.packageName===current?'selected':'')+'" onclick="selectApp(\''+a.packageName+'\')">'+a.appName+'<small style="color:rgba(255,255,255,0.5)">'+a.packageName+'</small></div>';}).join('');
}

document.getElementById('appSearch').addEventListener('input',e=>{
const q=e.target.value.toLowerCase();
renderApps(allApps.filter(a=>a.appName.toLowerCase().includes(q)||a.packageName.toLowerCase().includes(q)));
});

function selectApp(pkg){
document.getElementById('externalAppPackage').value=pkg;
document.querySelectorAll('.app-item').forEach(el=>el.classList.remove('selected'));
event.target.closest('.app-item').classList.add('selected');
}

async function startApp(){
const pkg=document.getElementById('externalAppPackage').value;
if(!pkg){showToast('Select an app first','error');return;}
try{await fetch('/api/app/start',{method:'POST',headers,body:JSON.stringify({packageName:pkg})});showToast('Starting app...','success');}catch(e){showToast('Error','error');}
}

async function stopApp(){
try{await fetch('/api/app/stop',{method:'POST',headers});showToast('Stopping app...','success');}catch(e){showToast('Error','error');}
}

async function restartApp(){
try{await fetch('/api/app/restart',{method:'POST',headers});showToast('Restarting app...','success');}catch(e){showToast('Error','error');}
}

// APK Upload
const uploadArea=document.getElementById('uploadArea');
uploadArea.addEventListener('dragover',e=>{e.preventDefault();uploadArea.classList.add('dragover');});
uploadArea.addEventListener('dragleave',()=>uploadArea.classList.remove('dragover'));
uploadArea.addEventListener('drop',e=>{e.preventDefault();uploadArea.classList.remove('dragover');if(e.dataTransfer.files.length)uploadApkFile(e.dataTransfer.files[0]);});

function uploadApk(){const f=document.getElementById('apkFile').files[0];if(f)uploadApkFile(f);}

async function uploadApkFile(file){
if(!file.name.endsWith('.apk')){showToast('Please select an APK file','error');return;}
const status=document.getElementById('uploadStatus');
status.innerHTML='<span style="color:#00d4ff">Uploading...</span>';
const formData=new FormData();
formData.append('file',file);
try{
const r=await fetch('/api/upload-apk',{method:'POST',headers:{'Authorization':'Bearer '+token},body:formData});
const d=await r.json();
if(d.success){status.innerHTML='<span style="color:#4caf50">✓ '+d.message+'</span>';showToast('APK uploaded!','success');}
else{status.innerHTML='<span style="color:#f44336">✗ '+d.error+'</span>';}
}catch(e){status.innerHTML='<span style="color:#f44336">Upload failed</span>';}
}

async function loadSettings(){
try{
const r=await fetch('/api/settings',{headers});
if(r.status===401){logout();return;}
const s=await r.json();
document.getElementById('kioskUrl').value=s.kioskUrl||'';
document.getElementById('desktopMode').checked=s.desktopMode||false;
document.getElementById('externalAppMode').checked=s.externalAppMode||false;
document.getElementById('externalAppPackage').value=s.externalAppPackage||'';
document.getElementById('pinCode').value=s.pinCode||'';
document.getElementById('pinAppToScreen').checked=s.pinAppToScreen||false;
document.getElementById('autoReload').checked=s.autoReload!==false;
document.getElementById('autoReloadEnabled').checked=(s.autoReloadMinutes||0)>0;
document.getElementById('autoReloadMinutes').value=s.autoReloadMinutes||0;
document.getElementById('showStatusBar').checked=s.showStatusBar||false;
document.getElementById('showNavButtons').checked=s.showNavButtons||false;
document.getElementById('screenAlwaysOn').checked=s.screenAlwaysOn!==false;
document.getElementById('screenOffEnabled').checked=s.screenOffEnabled||false;
document.getElementById('screenOffUse24h').value=String(s.screenOffUse24h!==false);
document.getElementById('screenOffStart').value=s.screenOffStart||'22:00';
document.getElementById('screenOffEnd').value=s.screenOffEnd||'07:00';
document.getElementById('brightness').value=s.brightness||100;
document.getElementById('brightnessValue').textContent=(s.brightness||100)+'%';
document.getElementById('screensaverTimeout').value=s.screensaverTimeout||0;
document.getElementById('screensaverType').value=s.screensaverType||'black';
document.getElementById('screensaverUrl').value=s.screensaverUrl||'';
document.getElementById('testMode').checked=s.testMode||false;
document.getElementById('maxPinAttempts').value=s.maxPinAttempts||5;
document.getElementById('dailyRebootEnabled').checked=s.dailyRebootEnabled||false;
document.getElementById('dailyRebootTime').value=s.dailyRebootTime||'03:00';
document.getElementById('use24hFormat').checked=s.use24hFormat!==false;
document.getElementById('autoLaunch').checked=s.autoLaunch!==false;
document.getElementById('kioskMode').checked=s.kioskMode||false;
document.getElementById('kioskExitTaps').value=s.kioskExitTaps||7;
document.getElementById('httpsEnabled').checked=s.httpsEnabled||false;
toggleContentMode();
loadCertInfo();
}catch(e){showToast('Error loading','error');}
}

async function loadCertInfo(){
try{
const r=await fetch('/api/certificate/info',{headers});
const d=await r.json();
document.getElementById('certInfo').textContent=d.info||'No certificate';
document.getElementById('httpsEnabled').checked=d.httpsEnabled||false;
}catch(e){document.getElementById('certInfo').textContent='Error loading certificate info';}
}

async function generateCert(){
if(!confirm('Generate new self-signed certificate?\nThis will replace any existing certificate.'))return;
showToast('Generating certificate...','success');
try{
const r=await fetch('/api/certificate/generate',{method:'POST',headers});
const d=await r.json();
if(d.success){showToast('Certificate generated! Enable HTTPS and save.','success');loadCertInfo();}
else showToast(d.error||'Failed','error');
}catch(e){showToast('Error','error');}
}

async function deleteCert(){
if(!confirm('Delete certificate? HTTPS will be disabled.'))return;
try{
const r=await fetch('/api/certificate/delete',{method:'POST',headers});
const d=await r.json();
if(d.success){showToast('Certificate deleted','success');document.getElementById('httpsEnabled').checked=false;loadCertInfo();}
else showToast(d.error||'Failed','error');
}catch(e){showToast('Error','error');}
}

function toggleHttps(){
const enabled=document.getElementById('httpsEnabled').checked;
if(enabled){showToast('HTTPS enabled. Save settings and restart server.','success');}
}

async function loadStatus(){
try{
const r=await fetch('/api/status',{headers});
if(r.status===401)return;
const s=await r.json();
document.getElementById('batteryLevel').textContent=s.batteryLevel+'%';
document.getElementById('wifiStatus').textContent=s.wifiConnected?'Connected':'Disconnected';
document.getElementById('deviceOwner').textContent=s.deviceOwner?'Yes':'No';
const u=Math.floor(s.uptime/1000/60),h=Math.floor(u/60),m=u%60;
document.getElementById('uptime').textContent=h+'h '+m+'m';
const ip=s.ipAddress||'Unknown';
const https=document.getElementById('httpsEnabled').checked;
const proto=https?'https':'http';
document.getElementById('deviceIp').textContent=ip;
document.getElementById('footerIp').textContent=ip;
document.getElementById('footerUrl').textContent=proto+'://'+ip+':2424';
}catch(e){}
}

async function saveSettings(){
const btn=document.getElementById('saveBtn');
btn.disabled=true;btn.textContent='Saving...';
const autoReloadOn=document.getElementById('autoReloadEnabled').checked;
const s={
kioskUrl:document.getElementById('kioskUrl').value,
desktopMode:document.getElementById('desktopMode').checked,
externalAppMode:document.getElementById('externalAppMode').checked,
externalAppPackage:document.getElementById('externalAppPackage').value,
pinCode:document.getElementById('pinCode').value,
pinAppToScreen:document.getElementById('pinAppToScreen').checked,
autoReload:document.getElementById('autoReload').checked,
autoReloadMinutes:autoReloadOn?parseInt(document.getElementById('autoReloadMinutes').value)||0:0,
showStatusBar:document.getElementById('showStatusBar').checked,
showNavButtons:document.getElementById('showNavButtons').checked,
screenAlwaysOn:document.getElementById('screenAlwaysOn').checked,
screenOffEnabled:document.getElementById('screenOffEnabled').checked,
screenOffUse24h:document.getElementById('screenOffUse24h').value==='true',
screenOffStart:document.getElementById('screenOffStart').value,
screenOffEnd:document.getElementById('screenOffEnd').value,
brightness:parseInt(document.getElementById('brightness').value),
screensaverTimeout:parseInt(document.getElementById('screensaverTimeout').value),
screensaverType:document.getElementById('screensaverType').value,
screensaverUrl:document.getElementById('screensaverUrl').value,
testMode:document.getElementById('testMode').checked,
maxPinAttempts:parseInt(document.getElementById('maxPinAttempts').value),
dailyRebootEnabled:document.getElementById('dailyRebootEnabled').checked,
dailyRebootTime:document.getElementById('dailyRebootTime').value,
use24hFormat:document.getElementById('use24hFormat').checked,
autoLaunch:document.getElementById('autoLaunch').checked,
kioskMode:document.getElementById('kioskMode').checked,
kioskExitTaps:parseInt(document.getElementById('kioskExitTaps').value)||7,
httpsEnabled:document.getElementById('httpsEnabled').checked,
webPassword:document.getElementById('webPassword').value
};
try{
const r=await fetch('/api/settings',{method:'POST',headers,body:JSON.stringify(s)});
const d=await r.json();
if(d.success){
showToast('Saved!','success');
document.getElementById('webPassword').value='';
if(s.httpsEnabled){showToast('Restart app for HTTPS to take effect','success');}
}
else showToast(d.error||'Error','error');
}catch(e){showToast('Connection error','error');}
finally{btn.disabled=false;btn.textContent='💾 Save Settings';}
}

async function setBrightness(v){try{await fetch('/api/brightness',{method:'POST',headers,body:JSON.stringify({brightness:v})});}catch(e){}}
async function reloadPage(){try{await fetch('/api/reload',{method:'POST',headers});showToast('Reloading...','success');}catch(e){showToast('Error','error');}}
async function rebootDevice(){if(!confirm('Reboot now?'))return;try{const r=await fetch('/api/reboot',{method:'POST',headers});const d=await r.json();showToast(d.message||'Rebooting...','success');}catch(e){showToast('Error','error');}}
function logout(){fetch('/api/logout',{method:'POST',headers}).catch(()=>{});localStorage.removeItem('token');location.href='/';}
function openLiveView(){location.href='/live?token='+token;}
function showToast(msg,type='success'){const t=document.getElementById('toast');t.textContent=msg;t.className='toast '+type+' show';setTimeout(()=>t.classList.remove('show'),3000);}
function updateTimeFormat(){}

document.getElementById('brightness').addEventListener('input',e=>{
document.getElementById('brightnessValue').textContent=e.target.value+'%';
setBrightness(parseInt(e.target.value));
});

let installedApps=[];
let selectedInstalledApp=null;

async function loadInstalledApps(){
try{
showToast('Loading installed apps...','success');
const r=await fetch('/api/apps',{headers});
const d=await r.json();
installedApps=d.apps||[];
renderInstalledApps(installedApps);
document.getElementById('installedAppsContainer').style.display='block';
}catch(e){showToast('Failed to load apps','error');}
}

function renderInstalledApps(apps){
const list=document.getElementById('installedAppList');
list.innerHTML=apps.map(function(a){return '<div class="app-item '+(selectedInstalledApp===a.packageName?'selected':'')+'" onclick="selectInstalledApp(\''+a.packageName+'\',this)">'+a.appName+'<small style="color:rgba(255,255,255,0.5)">'+a.packageName+'</small></div>';}).join('');
}

document.getElementById('installedAppSearch').addEventListener('input',e=>{
const q=e.target.value.toLowerCase();
renderInstalledApps(installedApps.filter(a=>a.appName.toLowerCase().includes(q)||a.packageName.toLowerCase().includes(q)));
});

function selectInstalledApp(pkg,el){
selectedInstalledApp=pkg;
document.querySelectorAll('#installedAppList .app-item').forEach(item=>item.classList.remove('selected'));
el.classList.add('selected');
}

async function uninstallSelectedApp(){
if(!selectedInstalledApp){showToast('Select an app first','error');return;}
if(!confirm('Uninstall '+selectedInstalledApp+'?'))return;
try{
const r=await fetch('/api/app/uninstall',{method:'POST',headers,body:JSON.stringify({packageName:selectedInstalledApp})});
const d=await r.json();
if(d.success){showToast('Uninstall initiated','success');selectedInstalledApp=null;loadInstalledApps();}
else showToast(d.error||'Failed','error');
}catch(e){showToast('Error','error');}
}

async function updateSelectedApp(){
if(!selectedInstalledApp){showToast('Select an app to update','error');return;}
document.getElementById('apkFile').click();
showToast('Select APK to update '+selectedInstalledApp,'success');
}

loadSettings();loadStatus();setInterval(loadStatus,10000);
</script>
</body>
</html>"""
}
