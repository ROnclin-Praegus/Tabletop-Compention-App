/**
 * TurnSyncManager for Tabletop RPG Companion Web
 * Local Wi-Fi sync client connecting to the Android Host (e.g. http://192.168.1.5:8080)
 * Zero external dependencies. Works offline-cached in Service Worker.
 */
(function(window) {
  'use strict';

  let myClientId = 'w-' + Math.random().toString(36).substring(2, 8);
  let hostAddress = '';
  let currentRoomCode = null;
  let isHost = false;
  let myParticipant = null;
  let roomState = null;
  let stateListeners = [];
  let pollIntervalId = null;

  function notifyStateChanged() {
    for (const cb of stateListeners) {
      try {
        cb(roomState, myParticipant, isHost);
      } catch (e) {
        console.error(e);
      }
    }
  }

  function formatHostUrl(url) {
    let clean = (url || '').trim();
    if (!clean) return '';
    if (!clean.startsWith('http://') && !clean.startsWith('https://')) {
      clean = 'http://' + clean;
    }
    // Remove trailing slash
    clean = clean.replace(/\/+$/, '');
    return clean;
  }

  async function postAction(action, payload) {
    if (!hostAddress) return null;
    try {
      const body = {
        action: action,
        payload: payload
      };
      const res = await fetch(hostAddress + '/api/battle/action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (res.ok) {
        const updated = await res.json();
        if (updated && updated.roomCode) {
          applyState(updated);
          return updated;
        }
      }
    } catch (e) {
      console.warn('TurnSync action error:', e);
    }
    return null;
  }

  async function fetchState() {
    if (!hostAddress) return;
    try {
      const res = await fetch(hostAddress + '/api/battle/state', {
        method: 'GET',
        cache: 'no-cache'
      });
      if (res.ok) {
        const state = await res.json();
        if (state && state.roomCode) {
          applyState(state);
        }
      }
    } catch (e) {
      // Ignore network hiccup during polling
    }
  }

  function applyState(newState) {
    roomState = newState;
    currentRoomCode = newState.roomCode;
    const found = (newState.participants || []).find(p => p.id === myClientId);
    if (found) {
      myParticipant = found;
    }
    notifyStateChanged();
  }

  function startPolling() {
    stopPolling();
    fetchState();
    pollIntervalId = setInterval(fetchState, 800);
  }

  function stopPolling() {
    if (pollIntervalId) {
      clearInterval(pollIntervalId);
      pollIntervalId = null;
    }
  }

  // --- Public API ---
  window.TurnSync = {
    getMyClientId: () => myClientId,
    getRoomCode: () => currentRoomCode,
    getHostAddress: () => hostAddress,
    isHost: () => isHost,
    getRoomState: () => roomState,
    getMyParticipant: () => myParticipant,

    addListener: function(cb) {
      if (!stateListeners.includes(cb)) stateListeners.push(cb);
    },
    removeListener: function(cb) {
      stateListeners = stateListeners.filter(l => l !== cb);
    },

    joinRoom: async function(targetHost, playerName, charId, hp, maxHp) {
      hostAddress = formatHostUrl(targetHost);
      if (!hostAddress) return false;
      try {
        localStorage.setItem('lastBattleHostIp', targetHost);
      } catch(e) {}

      isHost = false;
      myParticipant = {
        id: myClientId,
        name: playerName || 'Player',
        characterId: charId || null,
        slot: 1,
        isEnemy: false,
        currentHp: hp || null,
        maxHp: maxHp || null
      };

      const result = await postAction('JOIN', myParticipant);
      startPolling();
      return !!result;
    },

    claimSlot: function(slotNum) {
      const slot = parseInt(slotNum, 10);
      if (myParticipant) myParticipant.slot = slot;
      postAction('CLAIM_SLOT', { playerId: myClientId, slot: slot });
    },

    startBattle: function() {
      postAction('START', {});
    },

    endMyTurn: function() {
      postAction('END_TURN', { playerId: myClientId });
    },

    endEnemiesTurn: function() {
      postAction('END_ENEMIES', { playerId: myClientId });
    },

    forceNextTurn: function() {
      postAction('NEXT', {});
    },

    forcePrevTurn: function() {
      postAction('PREV', {});
    },

    resetBattle: function() {
      postAction('RESET', {});
    },

    leaveRoom: function() {
      stopPolling();
      if (hostAddress) {
        postAction('LEAVE', { playerId: myClientId });
      }
      hostAddress = '';
      currentRoomCode = null;
      isHost = false;
      myParticipant = null;
      roomState = null;
      notifyStateChanged();
    }
  };

})(window);
