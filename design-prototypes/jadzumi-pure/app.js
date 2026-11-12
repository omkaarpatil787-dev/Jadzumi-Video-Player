/* ==========================================================================
   JADZUMI PURE — INTERACTIVE PROTOTYPE ENGINE (JAVASCRIPT)
   Continuous Spatial Navigation, Device Adaptations & Feature Simulation
   ========================================================================== */

// --- Global Prototype State ---
const state = {
  currentView: 'home',
  previousView: 'home',
  libraryScrollPos: 0,
  device: 'tablet-landscape',
  accent: 'yellow',
  surface: 'amoled',
  spatialNavEnabled: true,
  isPlaying: true,
  playerProgress: 0.57, // 57%
  currentSeconds: 3438, // 57m 18s
  totalSeconds: 8040,   // 2h 14m 00s
  volume: 65,
  brightness: 78,
  aspectRatio: '2.40:1 Scope',
  playbackSpeed: '1.0×',
  audioTrack: 'English (E-AC-3 5.1)',
  subtitleTrack: 'English [SDH]',
  hudVisible: true,
  hudTimer: null,
  activeMovie: {
    title: 'The Silent Horizon',
    meta: '2026 · 2h 14m · Science Fiction',
    image: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1200&auto=format&fit=crop',
    synopsis: 'A quiet expedition across an unfamiliar world becomes a search for what it means to find home. Caught between vast silent dunes and unmapped starways, the crew must decide whether survival is enough.',
    progress: 57
  }
};

// --- Mock Library Dataset ---
const sampleMovies = [
  {
    title: 'The Silent Horizon',
    year: '2026',
    duration: '2h 14m',
    genre: 'Sci-Fi',
    badge: '4K HEVC',
    progress: 57,
    image: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop',
    synopsis: 'A quiet expedition across an unfamiliar world becomes a search for what it means to find home.'
  },
  {
    title: 'After the Rain',
    year: '2025',
    duration: '1h 48m',
    genre: 'Drama',
    badge: '1080p',
    progress: 0,
    image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600&auto=format&fit=crop',
    synopsis: 'In an isolated coastal sanctuary, old memories surface with the arrival of unannounced travelers.'
  },
  {
    title: 'Into the Wild',
    year: '2024',
    duration: '2h 05m',
    genre: 'Adventure',
    badge: 'HDR10',
    progress: 88,
    image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=600&auto=format&fit=crop',
    synopsis: 'An untamed wilderness tests the endurance of mountaineers confronting an unexpected blizzard.'
  },
  {
    title: 'Nocturne',
    year: '2025',
    duration: '1h 39m',
    genre: 'Thriller',
    badge: '4K DV',
    progress: 12,
    image: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600&auto=format&fit=crop',
    synopsis: 'Under neon reflections, an underground heist spirals into a mind game with the city watch.'
  },
  {
    title: 'Chronicle of Solitude',
    year: '2023',
    duration: '2h 22m',
    genre: 'Mystery',
    badge: '1080p',
    progress: 0,
    image: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop',
    synopsis: 'A deep-forest observatory uncovers cryptic radio signals echoing from deep within the earth.'
  },
  {
    title: 'Echoes of Aurora',
    year: '2026',
    duration: '1h 55m',
    genre: 'Fantasy',
    badge: '4K HEVC',
    progress: 34,
    image: 'https://images.unsplash.com/photo-1579033461380-adb47c3eb938?q=80&w=600&auto=format&fit=crop',
    synopsis: 'Mystical lights illuminate forgotten ruins on a desolate northern archipelago.'
  }
];

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  populateLibraryGrid();
  populateLibraryList();
  renderSearchResults(sampleMovies);
  startPlayerClock();
});

// ==========================================================================
// NAVIGATION & VIEW SWITCHING
// ==========================================================================
function navigateTo(viewId) {
  // If moving away from library, save scroll position
  const viewport = document.getElementById('viewViewport');
  if (state.currentView === 'library-grid' || state.currentView === 'library-list' || state.currentView === 'home') {
    state.libraryScrollPos = viewport.scrollTop;
  }

  // Deactivate player if returning to normal views
  const cinemaPlayer = document.getElementById('cinemaPlayer');
  if (viewId !== 'player') {
    cinemaPlayer.classList.remove('active');
  }

  // Hide all screens
  document.querySelectorAll('.screen-view').forEach(screen => screen.classList.remove('active'));

  // Activate target screen
  const target = document.getElementById(`view-${viewId}`);
  if (target) {
    target.classList.add('active');
  }

  // Update Rail & Bottom nav states
  document.querySelectorAll('.rail-item, .phone-nav-item').forEach(item => {
    item.classList.toggle('active', item.dataset.nav === viewId);
  });

  state.previousView = state.currentView;
  state.currentView = viewId;

  // Restore scroll position if returning to library or home
  if (viewId === 'library-grid' || viewId === 'library-list' || viewId === 'home') {
    setTimeout(() => {
      viewport.scrollTop = state.libraryScrollPos;
    }, 50);
  }

  // Sync Dropdown
  const select = document.getElementById('screenJumpSelect');
  if (select) select.value = viewId;
}

function jumpToScreen(screenId) {
  if (screenId === 'player') {
    launchPlayerDirectly();
  } else if (screenId.endsWith('-modal')) {
    openModal(screenId.replace('-modal', ''));
  } else if (screenId === 'storage-cleaner' || screenId === 'fix-match' || screenId === 'video-enhancer') {
    openModal(screenId);
  } else {
    closeModals();
    navigateTo(screenId);
  }
}

// ==========================================================================
// CONTINUOUS SPATIAL NAVIGATION (THE CORE JOURNEY)
// Library -> Details -> Player -> Return to Details -> Back to Library
// ==========================================================================
function startContinuousSpatialJourney() {
  openDetailsCard(
    'The Silent Horizon',
    '2026 · 2h 14m · Science Fiction',
    'https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1200&auto=format&fit=crop'
  );
}

function openDetailsCard(title, meta, image) {
  state.activeMovie.title = title;
  state.activeMovie.meta = meta;
  state.activeMovie.image = image;

  // Populate details view
  document.getElementById('detailsTitle').textContent = title;
  document.getElementById('detailsMeta').textContent = meta;
  document.getElementById('detailsHero').style.backgroundImage = `url('${image}')`;

  if (state.spatialNavEnabled) {
    const detailsHero = document.getElementById('detailsHero');
    detailsHero.style.transition = 'transform 420ms cubic-bezier(0.19, 1, 0.22, 1), opacity 420ms ease';
    detailsHero.style.transform = 'scale(0.96)';
    detailsHero.style.opacity = '0.7';

    navigateTo('details');

    requestAnimationFrame(() => {
      detailsHero.style.transform = 'scale(1)';
      detailsHero.style.opacity = '1';
    });
  } else {
    navigateTo('details');
  }
}

function returnFromDetailsToLibrary() {
  const detailsHero = document.getElementById('detailsHero');
  if (state.spatialNavEnabled) {
    detailsHero.style.transform = 'scale(0.96)';
    detailsHero.style.opacity = '0.7';
    setTimeout(() => {
      navigateTo(state.previousView || 'home');
      detailsHero.style.transform = 'scale(1)';
      detailsHero.style.opacity = '1';
    }, 200);
  } else {
    navigateTo(state.previousView || 'home');
  }
}

function startPlayTransition(isResume = false) {
  const cinemaPlayer = document.getElementById('cinemaPlayer');
  const playerCanvas = document.getElementById('playerVideoCanvas');
  const playerTitle = document.getElementById('playerTitle');

  playerTitle.textContent = state.activeMovie.title;
  playerCanvas.src = state.activeMovie.image;

  if (state.spatialNavEnabled) {
    cinemaPlayer.style.transform = 'scale(1.05)';
    cinemaPlayer.style.opacity = '0';
    cinemaPlayer.classList.add('active');

    requestAnimationFrame(() => {
      cinemaPlayer.style.transition = 'transform 450ms cubic-bezier(0.19, 1, 0.22, 1), opacity 450ms ease';
      cinemaPlayer.style.transform = 'scale(1)';
      cinemaPlayer.style.opacity = '1';
    });
  } else {
    cinemaPlayer.classList.add('active');
  }

  state.currentView = 'player';
  state.isPlaying = true;
  updatePlayPauseUI();
  resetHudAutoHide();
}

function launchPlayerDirectly() {
  startPlayTransition(true);
}

function returnFromPlayerToDetails() {
  const cinemaPlayer = document.getElementById('cinemaPlayer');
  if (state.spatialNavEnabled) {
    cinemaPlayer.style.transition = 'transform 380ms cubic-bezier(0.19, 1, 0.22, 1), opacity 380ms ease';
    cinemaPlayer.style.transform = 'scale(0.95)';
    cinemaPlayer.style.opacity = '0';
    setTimeout(() => {
      cinemaPlayer.classList.remove('active');
      navigateTo('details');
    }, 380);
  } else {
    cinemaPlayer.classList.remove('active');
    navigateTo('details');
  }
}

// ==========================================================================
// IMMERSIVE PLAYER INTERACTIONS & EDGE GESTURES
// ==========================================================================
function togglePlayPause() {
  state.isPlaying = !state.isPlaying;
  updatePlayPauseUI();
  resetHudAutoHide();
}

function updatePlayPauseUI() {
  const icon = document.getElementById('playPauseIcon');
  if (state.isPlaying) {
    // Pause Icon
    icon.innerHTML = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
  } else {
    // Play Icon
    icon.innerHTML = '<path d="M8 5v14l11-7z"/>';
  }
}

function simulateSeek(deltaSeconds) {
  state.currentSeconds = Math.max(0, Math.min(state.totalSeconds, state.currentSeconds + deltaSeconds));
  updateTimelineUI();
  resetHudAutoHide();

  // Trigger edge gesture seek feedback
  if (deltaSeconds < 0) {
    triggerEdgeFeedback('left', `−10s`);
  } else {
    triggerEdgeFeedback('right', `+10s`);
  }
}

function handleTimelineClick(event) {
  const hitbox = document.getElementById('timelineHitbox');
  const rect = hitbox.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const ratio = Math.max(0, Math.min(1, clickX / rect.width));

  state.currentSeconds = Math.floor(ratio * state.totalSeconds);
  updateTimelineUI();
  resetHudAutoHide();
}

function updateTimelineUI() {
  const progressRatio = state.currentSeconds / state.totalSeconds;
  const fill = document.getElementById('timelineFillBar');
  const thumb = document.getElementById('timelineThumb');
  const label = document.getElementById('currentTimeLabel');

  const percent = (progressRatio * 100).toFixed(1) + '%';
  fill.style.width = percent;
  thumb.style.left = percent;

  label.textContent = formatTime(state.currentSeconds);
}

function formatTime(sec) {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function startPlayerClock() {
  setInterval(() => {
    if (state.isPlaying && state.currentView === 'player') {
      state.currentSeconds = (state.currentSeconds + 1) % state.totalSeconds;
      updateTimelineUI();
    }
  }, 1000);
}

function togglePlayerHud() {
  const hud = document.getElementById('playerHudOverlay');
  state.hudVisible = !state.hudVisible;
  hud.classList.toggle('hidden', !state.hudVisible);
  if (state.hudVisible) resetHudAutoHide();
}

function resetHudAutoHide() {
  clearTimeout(state.hudTimer);
  const hud = document.getElementById('playerHudOverlay');
  hud.classList.remove('hidden');
  state.hudVisible = true;

  state.hudTimer = setTimeout(() => {
    if (state.isPlaying && state.currentView === 'player') {
      hud.classList.add('hidden');
      state.hudVisible = false;
    }
  }, 3200); // 3.2s smart auto-hide
}

// Minimal Edge Gesture Capsules (Left: Brightness, Right: Volume)
function triggerGesturePill(type) {
  if (type === 'brightness') {
    state.brightness = Math.min(100, state.brightness + 5);
    if (state.brightness > 100) state.brightness = 40;
    const pill = document.getElementById('leftBrightnessPill');
    document.getElementById('brightnessMeterFill').style.width = `${state.brightness}%`;
    document.getElementById('brightnessValueText').textContent = `${state.brightness}%`;
    showGesturePill(pill);
  } else {
    state.volume = Math.min(100, state.volume + 5);
    if (state.volume > 100) state.volume = 30;
    const pill = document.getElementById('rightVolumePill');
    document.getElementById('volumeMeterFill').style.width = `${state.volume}%`;
    document.getElementById('volumeValueText').textContent = `${state.volume}%`;
    showGesturePill(pill);
  }
}

function showGesturePill(pillElement) {
  pillElement.classList.add('visible');
  clearTimeout(pillElement._hideTimer);
  pillElement._hideTimer = setTimeout(() => {
    pillElement.classList.remove('visible');
  }, 1800);
}

function triggerEdgeFeedback(side, text) {
  const pill = side === 'left' ? document.getElementById('leftBrightnessPill') : document.getElementById('rightVolumePill');
  showGesturePill(pill);
}

function cycleAspectRatio() {
  const modes = ['2.40:1 Scope', '16:9 Fit', 'Stretch', 'Original'];
  const nextIdx = (modes.indexOf(state.aspectRatio) + 1) % modes.length;
  state.aspectRatio = modes[nextIdx];
  document.getElementById('aspectLabel').textContent = state.aspectRatio;
}

function setPlaybackSpeed(speed) {
  state.playbackSpeed = speed;
  document.getElementById('speedLabel').textContent = `${speed} Speed`;
  closeModals();
}

function selectAudioTrack(card, name) {
  document.querySelectorAll('#modal-audio .option-item-card').forEach(c => c.classList.remove('selected'));
  card.classList.add('selected');
  state.audioTrack = name;
  setTimeout(closeModals, 200);
}

function selectSubtitleTrack(card, name) {
  document.querySelectorAll('#modal-subtitle .option-item-card').forEach(c => c.classList.remove('selected'));
  card.classList.add('selected');
  state.subtitleTrack = name;
  setTimeout(closeModals, 200);
}

// ==========================================================================
// LIBRARY RENDERING & SEARCH
// ==========================================================================
function populateLibraryGrid() {
  const container = document.getElementById('libraryGridContainer');
  if (!container) return;

  container.innerHTML = sampleMovies.map(movie => `
    <div class="poster-card" onclick="openDetailsCard('${movie.title}', '${movie.year} · ${movie.duration} · ${movie.genre}', '${movie.image}')">
      <div class="poster-image-wrap">
        <img src="${movie.image}" alt="${movie.title}">
        <span class="poster-badge">${movie.badge}</span>
        ${movie.progress > 0 ? `<div class="poster-progress"><div class="poster-progress-bar" style="width: ${movie.progress}%;"></div></div>` : ''}
      </div>
      <div class="poster-title">${movie.title}</div>
      <div class="poster-subtitle">${movie.year} · ${movie.genre}</div>
    </div>
  `).join('');
}

function populateLibraryList() {
  const container = document.getElementById('libraryListContainer');
  if (!container) return;

  container.innerHTML = sampleMovies.map(movie => `
    <div class="list-row-item" onclick="openDetailsCard('${movie.title}', '${movie.year} · ${movie.duration} · ${movie.genre}', '${movie.image}')">
      <img class="list-thumb" src="${movie.image}" alt="${movie.title}">
      <div class="list-info">
        <div class="list-title">${movie.title}</div>
        <div class="list-meta">
          <span>${movie.year}</span> · 
          <span>${movie.duration}</span> · 
          <span class="list-badge">${movie.badge}</span>
        </div>
      </div>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
    </div>
  `).join('');
}

function setFilter(button, filterType) {
  document.querySelectorAll('.filter-pills .filter-pill').forEach(btn => btn.classList.remove('active'));
  button.classList.add('active');

  const filtered = sampleMovies.filter(m => {
    if (filterType === 'movies') return !m.title.includes('Station');
    if (filterType === 'tv') return m.title.includes('Station');
    if (filterType === 'unwatched') return m.progress === 0;
    return true;
  });

  const gridContainer = document.getElementById('libraryGridContainer');
  if (gridContainer) {
    gridContainer.innerHTML = filtered.map(movie => `
      <div class="poster-card" onclick="openDetailsCard('${movie.title}', '${movie.year} · ${movie.duration} · ${movie.genre}', '${movie.image}')">
        <div class="poster-image-wrap">
          <img src="${movie.image}" alt="${movie.title}">
          <span class="poster-badge">${movie.badge}</span>
          ${movie.progress > 0 ? `<div class="poster-progress"><div class="poster-progress-bar" style="width: ${movie.progress}%;"></div></div>` : ''}
        </div>
        <div class="poster-title">${movie.title}</div>
        <div class="poster-subtitle">${movie.year} · ${movie.genre}</div>
      </div>
    `).join('');
  }
}

function handleSearch(query) {
  const q = query.trim().toLowerCase();
  const matched = sampleMovies.filter(m => m.title.toLowerCase().includes(q) || m.genre.toLowerCase().includes(q));
  renderSearchResults(matched);
}

function clearSearch() {
  const input = document.getElementById('universalSearchInput');
  input.value = '';
  renderSearchResults(sampleMovies);
}

function renderSearchResults(movies) {
  const container = document.getElementById('searchResultsContainer');
  const countLabel = document.getElementById('searchCountLabel');
  if (!container) return;

  countLabel.textContent = `Results (${movies.length})`;
  container.innerHTML = movies.map(movie => `
    <div class="list-row-item" onclick="openDetailsCard('${movie.title}', '${movie.year} · ${movie.duration} · ${movie.genre}', '${movie.image}')">
      <img class="list-thumb" src="${movie.image}" alt="${movie.title}">
      <div class="list-info">
        <div class="list-title">${movie.title}</div>
        <div class="list-meta">${movie.year} · ${movie.duration} · <span class="list-badge">${movie.badge}</span></div>
      </div>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
    </div>
  `).join('');
}

function simulateRescan() {
  const icon = document.getElementById('rescanIcon');
  icon.style.transition = 'transform 1s linear';
  icon.style.transform = 'rotate(720deg)';
  setTimeout(() => {
    icon.style.transition = 'none';
    icon.style.transform = 'rotate(0deg)';
    alert('Library scan completed. 42 titles verified.');
  }, 1000);
}

// ==========================================================================
// PROTOTYPE CHROME SETTINGS (DEVICE, ACCENT & SURFACE MODES)
// ==========================================================================
function setDevice(deviceType) {
  const frame = document.getElementById('deviceFrame');
  frame.setAttribute('data-device', deviceType);
  state.device = deviceType;

  document.querySelectorAll('[data-device-btn]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-device-btn') === deviceType);
  });
}

function setAccent(accentName) {
  document.documentElement.setAttribute('data-theme', accentName);
  state.accent = accentName;

  document.querySelectorAll('[data-accent-btn]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-accent-btn') === accentName);
  });
}

function setSurface(surfaceName) {
  document.documentElement.setAttribute('data-surface', surfaceName);
  state.surface = surfaceName;

  document.querySelectorAll('[data-surface-btn]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-surface-btn') === surfaceName);
  });
}

function toggleSpatialNav() {
  state.spatialNavEnabled = !state.spatialNavEnabled;
  const btn = document.getElementById('spatialNavBtn');
  const toggle = document.getElementById('settingsSpatialToggle');

  btn.classList.toggle('active', state.spatialNavEnabled);
  btn.textContent = state.spatialNavEnabled ? '✨ Animated (420ms)' : '⚡ Reduced Motion';
  if (toggle) toggle.checked = state.spatialNavEnabled;
}

// ==========================================================================
// MODAL CONTROLS
// ==========================================================================
function openModal(modalId) {
  closeModals();
  const modal = document.getElementById(`modal-${modalId}`);
  if (modal) {
    modal.classList.add('active');
  }
}

function closeModals() {
  document.querySelectorAll('.modal-backdrop').forEach(modal => modal.classList.remove('active'));
}
