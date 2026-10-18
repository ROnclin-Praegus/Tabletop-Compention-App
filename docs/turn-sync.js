/**
 * TurnSyncManager for Tabletop RPG Companion Web
 * Micro-MQTT 3.1.1 client over WebSocket (wss://test.mosquitto.org:8081)
 * Zero external dependencies. Works offline-cached in Service Worker.
 */
(function(window) {
  'use strict';

  const WSS_URL = 'wss://test.mosquitto.org:8081';
  const TOPIC_PREFIX = 'tabletop-rpg/rooms/';

  let ws = null;
  let pingTimer = null;
  let myClientId = 'w-' + Math.random().toString(36).substring(2, 8);
  let currentRoomCode = null;
  let isHost = false;
  let myParticipant = null;
  let roomState = null;
  let stateListeners = [];

  function generateRoomCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
  }

  // --- Micro-MQTT 3.1.1 Binary Protocol Helpers ---
  function encodeString(str) {
    const encoder = new TextEncoder();
    const bytes = encoder.encode(str);
    const buf = new Uint8Array(bytes.length + 2);
    buf[0] = (bytes.length >> 8) & 0xff;
    buf[1] = bytes.length & 0xff;
    buf.set(bytes, 2);
    return buf;
  }

  function concatBuffers(bufs) {
    let totalLen = bufs.reduce((sum, b) => sum + b.length, 0);
    let res = new Uint8Array(totalLen);
    let offset = 0;
    for (const b of bufs) {
      res.set(b, offset);
      offset += b.length;
    }
    return res;
  }

  function encodeRemainingLength(len) {
    const bytes = [];
    do {
      let digit = len % 128;
      len = Math.floor(len / 128);
      if (len > 0) digit = digit | 0x80;
      bytes.push(digit);
    } while (len > 0);
    return new Uint8Array(bytes);
  }

  function buildConnectPacket(clientId) {
    const protoName = encodeString('MQTT');
    const protoLevel = new Uint8Array([0x04]); // 3.1.1
    const flags = new Uint8Array([0x02]); // clean session
    const keepAlive = new Uint8Array([0x00, 0x1e]); // 30 sec
    const clientPayload = encodeString(clientId);

    const varHeaderAndPayload = concatBuffers([protoName, protoLevel, flags, keepAlive, clientPayload]);
    const remLen = encodeRemainingLength(varHeaderAndPayload.length);
    const header = new Uint8Array([0x10]);

    return concatBuffers([header, remLen, varHeaderAndPayload]);
  }

  function buildSubscribePacket(topic, packetId) {
    const pId = new Uint8Array([(packetId >> 8) & 0xff, packetId & 0xff]);
    const topicBytes = encodeString(topic);
    const qos = new Uint8Array([0x00]);

    const payload = concatBuffers([pId, topicBytes, qos]);
    const remLen = encodeRemainingLength(payload.length);
    const header = new Uint8Array([0x82]);

    return concatBuffers([header, remLen, payload]);
  }

  function buildPublishPacket(topic, messageStr) {
    const topicBytes = encodeString(topic);
    const encoder = new TextEncoder();
    const msgBytes = encoder.encode(messageStr);

    const payload = concatBuffers([topicBytes, msgBytes]);
    const remLen = encodeRemainingLength(payload.length);
    const header = new Uint8Array([0x30]); // QoS 0

    return concatBuffers([header, remLen, payload]);
  }

  function buildPingPacket() {
    return new Uint8Array([0xc0, 0x00]);
  }

  function parseIncoming(data) {
    const bytes = new Uint8Array(data);
    if (bytes.length < 2) return;
    const packetType = (bytes[0] >> 4) & 0x0f;

    // PUBLISH packet
    if (packetType === 3) {
      let idx = 1;
      let multiplier = 1;
      let remLen = 0;
      let digit;
      do {
        digit = bytes[idx++];
        remLen += (digit & 0x7f) * multiplier;
        multiplier *= 128;
      } while ((digit & 0x80) !== 0 && idx < bytes.length);

      const topicLen = (bytes[idx] << 8) | bytes[idx + 1];
      idx += 2;
      const topicBytes = bytes.slice(idx, idx + topicLen);
      idx += topicLen;

      const payloadBytes = bytes.slice(idx);
      const decoder = new TextDecoder('utf-8');
      const text = decoder.decode(payloadBytes);
      handleMessage(text);
    }
  }

  function connect(roomCode, onOpen) {
    disconnect();
    currentRoomCode = roomCode;

    try {
      ws = new WebSocket(WSS_URL, ['mqtt']);
      ws.binaryType = 'arraybuffer';

      ws.onopen = function() {
        ws.send(buildConnectPacket(myClientId));
        // Subscribe to room topic
        setTimeout(() => {
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(buildSubscribePacket(TOPIC_PREFIX + roomCode, 1));
            startPingTimer();
            if (onOpen) onOpen();
          }
        }, 150);
      };

      ws.onmessage = function(event) {
        if (event.data instanceof ArrayBuffer) {
          parseIncoming(event.data);
        }
      };

      ws.onerror = function(err) {
        console.warn('TurnSync WebSocket error:', err);
      };

      ws.onclose = function() {
        stopPingTimer();
      };
    } catch (e) {
      console.error('TurnSync connection failed:', e);
    }
  }

  function startPingTimer() {
    stopPingTimer();
    pingTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(buildPingPacket());
      }
    }, 25000);
  }

  function stopPingTimer() {
    if (pingTimer) {
      clearInterval(pingTimer);
      pingTimer = null;
    }
  }

  function disconnect() {
    stopPingTimer();
    if (ws) {
      try {
        ws.close();
      } catch (e) {}
      ws = null;
    }
  }

  function sendEnvelope(type, payloadObj) {
    if (!ws || ws.readyState !== WebSocket.OPEN || !currentRoomCode) return;
    const envelope = {
      type: type,
      senderId: myClientId,
      roomCode: currentRoomCode,
      payloadJson: typeof payloadObj === 'string' ? payloadObj : JSON.stringify(payloadObj)
    };
    const packet = buildPublishPacket(TOPIC_PREFIX + currentRoomCode, JSON.stringify(envelope));
    ws.send(packet);
  }

  function broadcastSync() {
    if (!roomState) return;
    sendEnvelope('SYNC', roomState);
  }

  function notifyStateChanged() {
    for (const cb of stateListeners) {
      try {
        cb(roomState, myParticipant, isHost);
      } catch (e) {
        console.error(e);
      }
    }
  }

  function handleMessage(jsonStr) {
    try {
      const envelope = JSON.parse(jsonStr);
      if (envelope.roomCode !== currentRoomCode) return;

      switch (envelope.type) {
        case 'SYNC': {
          const syncedState = typeof envelope.payloadJson === 'string' ? JSON.parse(envelope.payloadJson) : envelope.payloadJson;
          if (syncedState) {
            roomState = syncedState;
            const found = roomState.participants.find(p => p.id === myClientId);
            if (found) myParticipant = found;
            notifyStateChanged();
          }
          break;
        }

        case 'JOIN': {
          if (isHost && roomState) {
            const newP = typeof envelope.payloadJson === 'string' ? JSON.parse(envelope.payloadJson) : envelope.payloadJson;
            const existing = roomState.participants.filter(p => p.id !== newP.id);
            const playerSlots = existing.filter(p => !p.isEnemy).map(p => p.slot);
            const nextSlot = (playerSlots.length > 0 ? Math.max(...playerSlots) : 0) + 1;
            newP.slot = nextSlot;
            roomState.participants = [...existing, newP];
            broadcastSync();
            notifyStateChanged();
          }
          break;
        }

        case 'CLAIM_SLOT': {
          if (isHost && roomState) {
            const data = typeof envelope.payloadJson === 'string' ? JSON.parse(envelope.payloadJson) : envelope.payloadJson;
            const pId = data.playerId;
            const newSlot = parseInt(data.slot, 10);
            roomState.participants = roomState.participants.map(p => {
              if (p.id === pId) return { ...p, slot: newSlot };
              return p;
            });
            broadcastSync();
            notifyStateChanged();
          }
          break;
        }

        case 'END_TURN': {
          if (isHost && roomState) {
            advanceTurnInternal();
          }
          break;
        }

        case 'END_ENEMIES': {
          if (isHost && roomState) {
            advanceEnemiesInternal();
          }
          break;
        }

        case 'LEAVE': {
          if (isHost && roomState) {
            const data = typeof envelope.payloadJson === 'string' ? JSON.parse(envelope.payloadJson) : envelope.payloadJson;
            roomState.participants = roomState.participants.filter(p => p.id !== data.playerId);
            broadcastSync();
            notifyStateChanged();
          }
          break;
        }
      }
    } catch (e) {
      console.warn('Error parsing incoming message:', e);
    }
  }

  function advanceTurnInternal() {
    if (!roomState) return;
    const playerSlots = roomState.participants.filter(p => !p.isEnemy).map(p => p.slot).sort((a, b) => a - b);
    if (playerSlots.length === 0) return;

    const currIdx = playerSlots.indexOf(roomState.activeSlot);
    if (currIdx >= 0 && currIdx < playerSlots.length - 1) {
      // Advance to next player
      roomState.activeSlot = playerSlots[currIdx + 1];
      roomState.status = 'BATTLE';
    } else {
      // Last player finished -> It is now the Enemies' Turn!
      roomState.status = 'ENEMIES';
      roomState.activeSlot = 999;
    }
    broadcastSync();
    notifyStateChanged();
  }

  function advanceEnemiesInternal() {
    if (!roomState) return;
    const playerSlots = roomState.participants.filter(p => !p.isEnemy).map(p => p.slot).sort((a, b) => a - b);
    const firstSlot = playerSlots.length > 0 ? playerSlots[0] : 1;

    roomState.round = (roomState.round || 1) + 1;
    roomState.activeSlot = firstSlot;
    roomState.status = 'BATTLE';

    broadcastSync();
    notifyStateChanged();
  }

  // --- Public API ---
  window.TurnSync = {
    getMyClientId: () => myClientId,
    getRoomCode: () => currentRoomCode,
    isHost: () => isHost,
    getRoomState: () => roomState,
    getMyParticipant: () => myParticipant,

    addListener: function(cb) {
      if (!stateListeners.includes(cb)) stateListeners.push(cb);
    },
    removeListener: function(cb) {
      stateListeners = stateListeners.filter(l => l !== cb);
    },

    createRoom: function(hostName, charId, hp, maxHp) {
      const code = generateRoomCode();
      isHost = true;
      myParticipant = {
        id: myClientId,
        name: hostName || 'Player 1',
        characterId: charId || null,
        slot: 1,
        isEnemy: false,
        currentHp: hp || null,
        maxHp: maxHp || null
      };

      roomState = {
        roomCode: code,
        hostId: myClientId,
        round: 1,
        activeSlot: 1,
        status: 'LOBBY',
        participants: [myParticipant]
      };

      connect(code, () => {
        broadcastSync();
        notifyStateChanged();
      });
      return code;
    },

    joinRoom: function(code, playerName, charId, hp, maxHp) {
      const cleanCode = (code || '').trim().toUpperCase();
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

      connect(cleanCode, () => {
        sendEnvelope('JOIN', myParticipant);
      });
    },

    claimSlot: function(slotNum) {
      const slot = parseInt(slotNum, 10);
      if (!myParticipant) return;
      myParticipant.slot = slot;

      if (isHost && roomState) {
        roomState.participants = roomState.participants.map(p => {
          if (p.id === myClientId) return { ...p, slot: slot };
          return p;
        });
        broadcastSync();
        notifyStateChanged();
      } else {
        sendEnvelope('CLAIM_SLOT', { playerId: myClientId, slot: slot });
      }
    },

    startBattle: function() {
      if (!isHost || !roomState) return;
      const playerSlots = roomState.participants.filter(p => !p.isEnemy).map(p => p.slot).sort((a, b) => a - b);
      roomState.status = 'BATTLE';
      roomState.round = 1;
      roomState.activeSlot = playerSlots.length > 0 ? playerSlots[0] : 1;
      broadcastSync();
      notifyStateChanged();
    },

    endMyTurn: function() {
      if (isHost) {
        advanceTurnInternal();
      } else {
        sendEnvelope('END_TURN', { playerId: myClientId });
      }
    },

    endEnemiesTurn: function() {
      if (isHost) {
        advanceEnemiesInternal();
      } else {
        sendEnvelope('END_ENEMIES', { playerId: myClientId });
      }
    },

    forceNextTurn: function() {
      if (!isHost) return;
      if (roomState.status === 'BATTLE') advanceTurnInternal();
      else if (roomState.status === 'ENEMIES') advanceEnemiesInternal();
    },

    forcePrevTurn: function() {
      if (!isHost || !roomState) return;
      const playerSlots = roomState.participants.filter(p => !p.isEnemy).map(p => p.slot).sort((a, b) => a - b);
      if (playerSlots.length === 0) return;

      if (roomState.status === 'ENEMIES') {
        roomState.status = 'BATTLE';
        roomState.activeSlot = playerSlots[playerSlots.length - 1];
      } else {
        const currIdx = playerSlots.indexOf(roomState.activeSlot);
        if (currIdx > 0) {
          roomState.activeSlot = playerSlots[currIdx - 1];
        }
      }
      broadcastSync();
      notifyStateChanged();
    },

    resetBattle: function() {
      if (!isHost || !roomState) return;
      roomState.status = 'LOBBY';
      roomState.round = 1;
      roomState.activeSlot = 1;
      broadcastSync();
      notifyStateChanged();
    },

    leaveRoom: function() {
      if (currentRoomCode) {
        sendEnvelope('LEAVE', { playerId: myClientId });
      }
      disconnect();
      currentRoomCode = null;
      isHost = false;
      myParticipant = null;
      roomState = null;
      notifyStateChanged();
    }
  };

})(window);
