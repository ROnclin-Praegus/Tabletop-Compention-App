// Helpers that deal with untrusted hero data. Kept in their own file so tests.html can check them.

    function escapeHtml(str) {
      if (!str) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }

// Makes text safe as an argument inside an inline handler: onclick="f(${jsArg(text)})".
// JSON.stringify quotes it as a JS string; escapeHtml stops a " from ending the attribute.
function jsArg(str) {
  return escapeHtml(JSON.stringify(String(str == null ? '' : str)));
}

    // Turns untrusted hero data (saved, imported) into a safe, complete hero.
    // Returns null if it is not a usable hero. Fixes missing arrays, wrong types, and unsafe ids.
    function normalizeCharacter(raw) {
      if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null;
      if (typeof raw.name !== 'string' || !raw.name.trim()) return null;
      const newId = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
      const safeId = v => String(v == null ? '' : v).replace(/[^A-Za-z0-9_-]/g, '') || newId();
      const num = v => (Number.isFinite(Number(v)) ? Number(v) : 0);
      const fixNums = (o, keys) => { keys.forEach(k => { if (o[k] != null) o[k] = num(o[k]); }); };
      const fixStrs = (o, keys) => { keys.forEach(k => { if (o[k] != null && typeof o[k] !== 'string') o[k] = String(o[k]); }); };
      const fixMods = o => {
        if (o.statModifiers == null) return;
        const m = {};
        if (typeof o.statModifiers === 'object' && !Array.isArray(o.statModifiers)) {
          Object.entries(o.statModifiers).forEach(([k, v]) => { m[k] = num(v); });
        }
        o.statModifiers = m;
      };
      const list = (v, fn) => (Array.isArray(v) ? v : [])
        .filter(x => x && typeof x === 'object' && !Array.isArray(x))
        .map(x => { const o = { ...x, id: safeId(x.id) }; fn(o); return o; });

      const c = { ...raw, id: safeId(raw.id) };
      fixStrs(c, ['race']);
      if (c.race == null) c.race = '';
      // Only inline image data is kept. A web address would be fetched as soon as the hero list is drawn.
      if (typeof c.imageUri !== 'string' || !c.imageUri.startsWith('data:image/')) c.imageUri = null;
      fixNums(c, ['level', 'maxHp', 'currentHp', 'boostHp', 'maxBoostHp', 'boostHpTurns', 'maxMp', 'currentMp',
        'gold', 'strength', 'intelligence', 'endurance', 'spirit', 'finesse', 'charisma']);
      c.inventory = list(raw.inventory, o => {
        fixStrs(o, ['name', 'category', 'attackDamage', 'specialEffectsText', 'onHandSpecialEffectsText']);
        fixNums(o, ['attackCount', 'spellDamageModifier', 'armorSave', 'wardSave']);
        fixMods(o);
      });
      c.abilities = list(raw.abilities, o => { fixStrs(o, ['name', 'description', 'category']); });
      c.notes = list(raw.notes, o => { fixStrs(o, ['title', 'content', 'tag']); });
      c.effects = list(raw.effects, o => {
        fixStrs(o, ['name', 'diceFormula', 'damageType', 'changeMode', 'effectType', 'targetStat']);
        // The Android app writes PHYSICAL, the web app physical: the web app only understands lowercase.
        if (o.damageType) o.damageType = o.damageType.toLowerCase();
        // These words are put into the page and compared in code: keep only the known ones.
        const oneOf = (v, allowed, fallback) => (allowed.includes(v) ? v : fallback);
        o.damageType = oneOf(o.damageType, ['physical', 'magical', 'unsaveable'], 'unsaveable');
        o.changeMode = oneOf(o.changeMode, ['DMG', 'HEAL', 'MIXED'], 'DMG');
        o.effectType = oneOf(o.effectType, ['STAT_MODIFIER', 'HP_CHANGE_PER_ROUND', 'MP_CHANGE_PER_ROUND'], 'STAT_MODIFIER');
        fixNums(o, ['value', 'roundsRemaining']);
        fixMods(o);
      });
      return c;
    }

// Small stable hash, used to give each distinct unreadable save its own backup key.
function hashText(text) {
  let h = 5381;
  for (let i = 0; i < text.length; i++) h = ((h * 33) ^ text.charCodeAt(i)) >>> 0;
  return h.toString(36);
}

function getTotalArmor(c) {
  if (!c || !c.inventory) return 0;
  return c.inventory
    .filter(i => i.isEquipped && (i.hasArmor || i.armorSave > 0))
    .reduce((sum, i) => sum + (parseInt(i.armorSave) || 0), 0);
}

function getTotalWard(c) {
  if (!c || !c.inventory) return 0;
  return c.inventory
    .filter(i => i.isEquipped && (i.hasWard || i.wardSave > 0))
    .reduce((sum, i) => sum + (parseInt(i.wardSave) || 0), 0);
}


// Applies damage to a hero and changes the hero in place.
// Order: physical -> boost armor, then armor; magical -> boost ward, then ward; unsaveable skips both.
// What is left goes to boost HP, then HP. Returns how much each part lost (for the floating numbers).
function absorbDamage(c, amount, type) {
  let remaining = amount;
  const take = have => {
    const used = Math.min(remaining, Math.max(0, have));
    remaining -= used;
    return used;
  };
  c.currentArmor = (c.currentArmor === null || c.currentArmor === undefined) ? getTotalArmor(c) : c.currentArmor;
  c.currentWard = (c.currentWard === null || c.currentWard === undefined) ? getTotalWard(c) : c.currentWard;
  c.boostArmor = c.boostArmor || 0;
  c.boostWard = c.boostWard || 0;
  c.boostHp = c.boostHp || 0;

  let armorChange = 0;
  let wardChange = 0;
  if (type === 'physical') {
    let used = take(c.boostArmor); c.boostArmor -= used; armorChange += used;
    used = take(c.currentArmor); c.currentArmor -= used; armorChange += used;
  } else if (type === 'magical') {
    let used = take(c.boostWard); c.boostWard -= used; wardChange += used;
    used = take(c.currentWard); c.currentWard -= used; wardChange += used;
  }

  const hpBefore = c.currentHp + c.boostHp;
  const usedBoost = take(c.boostHp);
  c.boostHp -= usedBoost;
  c.currentHp = Math.max(0, c.currentHp - remaining);
  return { armorChange, wardChange, hpChange: hpBefore - (c.currentHp + c.boostHp) };
}

// Call after the hero's gear changed. Armor and ward that already took damage stay damaged (only capped
// to the new total), so changing gear mid-round does not heal them. null means "untouched = full" and stays full.
function capDefensesToGear(c) {
  if (c.currentArmor !== null && c.currentArmor !== undefined) c.currentArmor = Math.min(c.currentArmor, getTotalArmor(c));
  if (c.currentWard !== null && c.currentWard !== undefined) c.currentWard = Math.min(c.currentWard, getTotalWard(c));
}

// Plays one round for a hero and changes the hero in place. Same rules as tickRound() in
// app/.../model/RoundTick.kt (both run the cases in docs/rule-cases.json).
// maxHp, maxMp and spirit are the hero's effective values (they depend on gear and effects).
// diceRollResults maps an effect id to the rolled amount (negative = damage).
// Returns the messages for things that ran out, and how much was gained or lost (for the floating numbers).
function tickRound(c, maxHp, maxMp, spirit, diceRollResults) {
  const expired = [];
  const result = { expired, armorLost: 0, wardLost: 0, hpLost: 0, hpGained: 0 };
  c.currentArmor = getTotalArmor(c);
  c.currentWard = getTotalWard(c);

  const boosts = [
    { cur: 'boostHp', max: 'maxBoostHp', turns: 'boostHpTurns', reset: 'boostHpResetOnNextRound', end: 'Boost HP duration has ended!' },
    { cur: 'boostArmor', max: 'maxBoostArmor', turns: 'boostArmorTurns', reset: 'boostArmorResetOnNextRound', end: 'Armor Boost duration has ended!' },
    { cur: 'boostWard', max: 'maxBoostWard', turns: 'boostWardTurns', reset: 'boostWardResetOnNextRound', end: 'Ward Save Boost duration has ended!' }
  ];
  const maxOf = b => (c[b.max] !== undefined && c[b.max] !== null) ? c[b.max] : (c[b.cur] || 0);
  const turnsOf = b => (c[b.turns] !== undefined ? c[b.turns] : 0);

  // 1. A boost that resets each round goes back to its full amount while it is active.
  boosts.forEach(b => {
    const turns = turnsOf(b);
    if (maxOf(b) > 0 && c[b.reset] !== false && (turns > 0 || (turns === 0 && c[b.cur] > 0))) c[b.cur] = maxOf(b);
  });

  // 2. Spirit recharges MP (1 Spirit = +1 MP per round).
  if (maxMp > 0 && spirit > 0) c.currentMp = Math.min(maxMp, c.currentMp + spirit);

  // 3. Per-round HP/MP effects and their durations. A switched-off effect does nothing and does not run down.
  const rolled = e => (e.isDiceRoll && diceRollResults && Object.prototype.hasOwnProperty.call(diceRollResults, e.id))
    ? (parseInt(diceRollResults[e.id]) || 0)
    : (parseInt(e.value) || 0);
  const next = [];
  (c.effects || []).forEach(e => {
    if (e.isEnabled === false) { next.push(e); return; }
    if (e.effectType === 'HP_CHANGE_PER_ROUND') {
      const val = rolled(e);
      if (val > 0) {
        const old = c.currentHp;
        c.currentHp = Math.max(0, Math.min(maxHp, c.currentHp + val));
        result.hpGained += c.currentHp - old;
      } else if (val < 0) {
        const r = absorbDamage(c, Math.abs(val), e.damageType || 'unsaveable');
        result.armorLost += r.armorChange;
        result.wardLost += r.wardChange;
        result.hpLost += r.hpChange;
      }
    }
    if (e.effectType === 'MP_CHANGE_PER_ROUND' && maxMp > 0) {
      c.currentMp = Math.max(0, Math.min(maxMp, c.currentMp + rolled(e)));
    }
    const r = parseInt(e.roundsRemaining) || 0;
    if (r > 1) next.push({ ...e, roundsRemaining: r - 1 });
    else if (r === 1) expired.push(`Effect expired: ${e.name}`);
    else if (r === 0) next.push(e); // 0 = unlimited
  });
  c.effects = next;

  // 4. Boost durations count down.
  boosts.forEach(b => {
    const turns = turnsOf(b);
    if (turns > 1) c[b.turns] = turns - 1;
    else if (turns === 1) {
      c[b.turns] = 0; c[b.cur] = 0; c[b.max] = 0;
      expired.push(b.end);
    }
  });
  return result;
}
