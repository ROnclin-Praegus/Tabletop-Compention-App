import re

with open('docs/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

css_addition = '''
    @keyframes floatUpFadeOut {
      0% { opacity: 1; transform: translateY(0); }
      100% { opacity: 0; transform: translateY(-20px); }
    }
    .floating-damage {
      position: absolute;
      right: 0;
      bottom: 10px;
      color: var(--crimson-primary);
      font-weight: bold;
      font-size: 16px;
      animation: floatUpFadeOut 1.5s forwards;
      pointer-events: none;
      z-index: 10;
    }
'''
content = content.replace('    .hidden { display: none !important; }', '    .hidden { display: none !important; }' + css_addition)

content = content.replace('.bar-group {\\n      flex: 1;\\n    }', '.bar-group {\\n      flex: 1;\\n      position: relative;\\n    }')

js_function = '''
    function showFloatingDamage(fillId, amount) {
      if (amount <= 0) return;
      const fillEl = document.getElementById(fillId);
      if (!fillEl) return;
      const container = fillEl.parentElement.parentElement;
      const floatEl = document.createElement('div');
      floatEl.className = 'floating-damage';
      floatEl.innerText = '-' + amount;
      container.appendChild(floatEl);
      setTimeout(() => {
        if (floatEl.parentElement) floatEl.remove();
      }, 1500);
    }
'''

content = content.replace('function confirmApplyDamage() {', js_function + '\\n    function confirmApplyDamage() {')

armor_repl = '''
      if (selectedType === 'physical') {
        if (curArmor > 0) {
          if (rem <= curArmor) {
            showFloatingDamage('useArmorFill', rem);
            curArmor -= rem;
            rem = 0;
          } else {
            showFloatingDamage('useArmorFill', curArmor);
            rem -= curArmor;
            curArmor = 0;
          }
        }
      }
'''
content = re.sub(r'if \(selectedType === \\'physical\\'\\) \\{[\\s\\S]*?\\}\\s*\\}', armor_repl.strip(), content)

ward_repl = '''
      } else if (selectedType === 'magical') {
        if (curWard > 0) {
          if (rem <= curWard) {
            showFloatingDamage('useWardFill', rem);
            curWard -= rem;
            rem = 0;
          } else {
            showFloatingDamage('useWardFill', curWard);
            rem -= curWard;
            curWard = 0;
          }
        }
      }
'''
content = re.sub(r'\\} else if \\(selectedType === \\'magical\\'\\) \\{[\\s\\S]*?\\}\\s*\\}', ward_repl.strip(), content)

hp_repl = '''
      if (rem > 0) {
        showFloatingDamage('useHpFill', rem);
        c.currentHp = Math.max(0, c.currentHp - rem);
      }
'''
content = re.sub(r'if \\(rem > 0\\) \\{\\s*c\\.currentHp = Math\\.max\\(0, c\\.currentHp - rem\\);\\s*\\}', hp_repl.strip(), content)

with open('docs/index.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('done')
