const SPELLBOOKS_DATABASE = {
    "Magic: Water":  {
                         "type":  "magic",
                         "name":  "Magic: Water",
                         "displayName":  "Water Magic",
                         "aliases":  [
                                         "Magic: Water",
                                         "Water"
                                     ],
                         "spells":  [
                                        {
                                            "cost":  "1 MP",
                                            "description":  "Pull at an enemy for 1d4 damage, moves them closer to you, and leaves them bound for 1 turn, halving their attacks and movement.",
                                            "name":  "Water lasso",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1 MP",
                                            "description":  "Cleanse a container of water of all harmful material.",
                                            "name":  "Purify",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1 MP",
                                            "description":  "Grant yourself or a party member the ability to breathe underwater.",
                                            "name":  "Bubble",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1 MP/liter",
                                            "description":  "Conjure forth a desired quantity of plain water. Can be used to douse fire or cure the burn effect.",
                                            "name":  "Summon Water",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1 MP/pp",
                                            "description":  "Create watery tendrils that spout from your back. Can interact with light objects and transport items.",
                                            "name":  "Tendril",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "3 MP",
                                            "description":  "Part a body of water to create a path across. Is limited to bodies of water where the destination can be observed.",
                                            "name":  "Hannibal’s Gambit",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "2 MP",
                                            "description":  "Force water into a target’s lungs, inflicting 1d6+5 physical damage. Can only be used if target’s face is visible.",
                                            "name":  "Moist Lungs",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "2 MP",
                                            "description":  "Target a single enemy and hose them down with freezing cold water, slowing them to a fourth of their movement and reducing their finesse by 3. Must be within 8 tiles.",
                                            "name":  "Water hose",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "2 MP",
                                            "description":  "A protective bubble forms around the target, granting them +10 magical armor.",
                                            "name":  "Bubble shield",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "3 MP",
                                            "description":  "Boiling water bursts from the ground, doing 1d6+INT damage and causing an extra 1d3 physical damage upon landing.",
                                            "name":  "Geyser",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "3 MP",
                                            "description":  "1d6+INT damage to all targets in a 3*x block in front of the user + 1d3 knockback.",
                                            "name":  "Wave",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "5 MP",
                                            "description":  "Create a whirlpool in water, drawing all targets under a large blast to the center and doing 2d6+5 drowning damage.",
                                            "name":  "Whirlpool",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "2 MP",
                                            "description":  "Cause a violent rainstorm. Has RP effects, but also halves fire damage across the field.",
                                            "name":  "Torrent",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "2 MP",
                                            "description":  "While in water, launch yourself forward at thrice your regular speed.",
                                            "name":  "Jet",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "3 MP/pp",
                                            "description":  "More powerful tendril capable of transporting heavier goods, including people. Extends to a maximum of 10 meters.",
                                            "name":  "Empower tendril",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "5 MP",
                                            "description":  "A bubble of water forms around the target, doing 2d8+5+INT damage.",
                                            "name":  "Drowning",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "4 MP",
                                            "description":  "Your body instantly turns to water and leaks away, lending you amazing flexibility.",
                                            "name":  "Liquid form",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "4 MP",
                                            "description":  "An ability learnt from druids, which uses water to rapidly rust away steel constructions. This reduces the effectiveness of weapons and armor, and it can also disable machines.",
                                            "name":  "Rust steel",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "6 MP",
                                            "description":  "An avatar of the the great shark spirit Gawr appears, snapping its mighty jaw at targets in front of the caster. 3d6 magical damage to everything in a 2*3 block.",
                                            "name":  "Gawr’s Bite",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "3 MP",
                                            "description":  "Causes all water under a large blast marker to begin boiling. Everything within suffers 1d6+3 damage per turn.",
                                            "name":  "Boil water",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "Passive skill",
                                            "description":  "When you enter a body of water, you instantly transform into a mermaid. Improved swimming skill and compatible with Jet.",
                                            "name":  "Mermaid’s Grace",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "5MP",
                                            "description":  "A high-pressure beam of water that can cut through metal. Roll for FIN to break weapons and armor, or target a person to inflict 3d6+5 damage",
                                            "name":  "Water Jet Cutter",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "3 MP",
                                            "description":  "Sacrifice 3 MP to maintain a water elemental with 15 health that has access to two tier 2 spells per turn.",
                                            "name":  "Water Elemental",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "Passive",
                                            "description":  "A tedious and long process where an entire person is cleansed from harmful toxins. Takes an hour, but guarantees restoration from poisoning.",
                                            "name":  "Purify person",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "5 MP",
                                            "description":  "Turn any piece of ground under a small blast marker into a pool of water for 1 turn, granted that it is sufficiently flat to allow this. Anything in the circle falls in the water and is ejected the next turn, causing 2d4 damage. If it/they can’t swim, they also take 1d6 drowning damage for the duration of this spell.",
                                            "name":  "Turn water",
                                            "tier":  5
                                        }
                                    ]
                     },
    "Magic: Fire":  {
                        "type":  "magic",
                        "name":  "Magic: Fire",
                        "displayName":  "Fire Magic",
                        "aliases":  [
                                        "Magic: Fire",
                                        "Fire"
                                    ],
                        "spells":  [
                                       {
                                           "cost":  "1 MP",
                                           "description":  "Start a small fire on a visible surface.",
                                           "name":  "Ignite",
                                           "tier":  1
                                       },
                                       {
                                           "cost":  "Passive",
                                           "description":  "Cause the target’s body to feel warm, regardless of outside conditions. This can be used to fend off cold when suitable clothing is not available.",
                                           "name":  "Inner heat",
                                           "tier":  1
                                       },
                                       {
                                           "cost":  "1 MP",
                                           "description":  "Heats a container of water so much that it instantly begins cooking.",
                                           "name":  "Boil",
                                           "tier":  1
                                       },
                                       {
                                           "cost":  "2 MP",
                                           "description":  "A close-range attack that inflicts 2d4+int+strength+melee damage.",
                                           "name":  "Fire punch",
                                           "tier":  1
                                       },
                                       {
                                           "cost":  "1 MP",
                                           "description":  "Douse any small, nearby fires like candles, hearths and lanterns.",
                                           "name":  "Steal fire",
                                           "tier":  1
                                       },
                                       {
                                           "cost":  "3 MP",
                                           "description":  "3d4+int fire damage in a large blast",
                                           "name":  "Fireball",
                                           "tier":  2
                                       },
                                       {
                                           "cost":  "1 MP",
                                           "description":  "Become instantly aware of nearby sources of heat or fire.",
                                           "name":  "Detect heat",
                                           "tier":  2
                                       },
                                       {
                                           "cost":  "2 MP",
                                           "description":  "A sudden flash blinds all unprepared people on the field, reducing finesse by 2.",
                                           "name":  "Devil Flash",
                                           "tier":  2
                                       },
                                       {
                                           "cost":  "4 MP",
                                           "description":  "Inflict 2d8+5 damage per turn, so long as you can continue touching the target.",
                                           "name":  "Burning Hand",
                                           "tier":  2
                                       },
                                       {
                                           "cost":  "2 MP",
                                           "description":  "Block 3 tiles with a wall of fire, inflicting 2d6 damage to anyone passing through.",
                                           "name":  "Firewall",
                                           "tier":  2
                                       },
                                       {
                                           "cost":  "4 MP",
                                           "description":  "a 2d8+int large (2*3) explosion of fire damage right in front of you, knocking enemies back.",
                                           "name":  "Explosion!",
                                           "tier":  3
                                       },
                                       {
                                           "cost":  "3 MP",
                                           "description":  "Instantly turn a low level creature into a cooked version of itself, such as a roast chicken. Edible, but preparation will taste mediocre.",
                                           "name":  "Cook",
                                           "tier":  3
                                       },
                                       {
                                           "cost":  "3 MP",
                                           "description":  "+10 magic ward for 5 turns. Any fire magic inflicted on it whilst active will heal you.",
                                           "name":  "Fire ward",
                                           "tier":  3
                                       },
                                       {
                                           "cost":  "3 MP",
                                           "description":  "Cause the heat in any given contained space to rise by 10 degrees. Speed depends on size of the area.",
                                           "name":  "Heat area",
                                           "tier":  3
                                       },
                                       {
                                           "cost":  "4 MP",
                                           "description":  "A giant snake of fire that travels in a set direction. Inflicts 2d6+5+int damage. Wiggles such that it will hit enemies even when not in a perfect line.",
                                           "name":  "Fire Snake",
                                           "tier":  3
                                       },
                                       {
                                           "cost":  "5 MP",
                                           "description":  "3D6+int fire damage in a flame tile pattern. Leaves enemies burning for 1d6 per turn until extinguished.",
                                           "name":  "Flamethrower",
                                           "tier":  4
                                       },
                                       {
                                           "cost":  "5MP",
                                           "description":  "3d6+INT damage to a single target. Also melts an amount of armor equal to the damage inflicted.",
                                           "name":  "Infernal ball",
                                           "tier":  4
                                       },
                                       {
                                           "cost":  "5 MP",
                                           "description":  "Fill an area in a large blast marker with a wall of fire, forcing anyone to make a spirit check and take 3d6 fire damage to escape.",
                                           "name":  "Ring of Fire",
                                           "tier":  4
                                       },
                                       {
                                           "cost":  "5 MP",
                                           "description":  "Inflict 2d10+int damage to an opponent wearing steel armor.",
                                           "name":  "Heat metal",
                                           "tier":  4
                                       },
                                       {
                                           "cost":  "5 MP",
                                           "description":  "A skin-melting blast. Inflicts 3d8+3 damage in an area of 2 tiles around you.",
                                           "name":  "Facemelter",
                                           "tier":  4
                                       },
                                       {
                                           "cost":  "Passive",
                                           "description":  "Shed your physical constraints. Your body is consumed by permanent fire, rendering you invulnerable to fire, temperature, and physical damage. Requires a ritual to attain.",
                                           "name":  "Fireform",
                                           "tier":  5
                                       },
                                       {
                                           "cost":  "6MP",
                                           "description":  "Inflict 3d10+int damage unto a single target, while leaving them burning for 1d6 damage per turn afterward.",
                                           "name":  "Incinerate",
                                           "tier":  5
                                       },
                                       {
                                           "cost":  "4MP",
                                           "description":  "Every source of heat in the room converges unto you, healing you for 1d6+3 per consumed source.",
                                           "name":  "Advanced Steal Fire",
                                           "tier":  5
                                       },
                                       {
                                           "cost":  "6MP",
                                           "description":  "Spray someone with horrifically burning magma. Inflicts 2d8+int damage instantly and melt 10 armor. If this reduces armor below 0, inflict an additional 2d8 damage.",
                                           "name":  "Magma",
                                           "tier":  5
                                       },
                                       {
                                           "cost":  "7 MP",
                                           "description":  "Spawn a volcano that spews rocks, fire, and magma everywhere. The entire battlefield takes 2d6 damage per turn. Upon spawning, the volcano inflicts an additional 2d8 damage on anyone in its small blast radius spawn area + 1d3+3 knockback in a random direction.",
                                           "name":  "Volcano",
                                           "tier":  5
                                       }
                                   ]
                    },
    "Magic: Darkness":  {
                            "type":  "magic",
                            "name":  "Magic: Darkness",
                            "displayName":  "Darkness Magic",
                            "aliases":  [
                                            "Magic: Darkness",
                                            "Magic: Dark",
                                            "Darkness",
                                            "Dark"
                                        ],
                            "spells":  [
                                           {
                                               "cost":  "1MP/1HP",
                                               "description":  "A small blast of dark fog erupts from the caster clouding their position and reducing to hit by 2.",
                                               "name":  "Fog",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "2MP/2 HP",
                                               "description":  "2d4+int damage projectile that can be cast 3 times per turn.",
                                               "name":  "Dark orb",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "3MP/3HP",
                                               "description":  "mark a tile on the floor that, if crossed, explodes in a blast that does 1d8+int aoe damage.",
                                               "name":  "Dark trap",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "2MP/1HP",
                                               "description":  "A hand erupts from the caster, grabbing a foe and throwing them 1d3 space aside while doing 2d3 damage.",
                                               "name":  "Darkhand",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "1MP/1HP",
                                               "description":  "Fill a moderate-sized body of water with the taint of corruption, making it undrinkable and harmful to touch.",
                                               "name":  "Corrupt",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "5 HP",
                                               "description":  "Sacrifice 5HP of your own to maintain a small cretin that fires dark magic for 1d3 damage per turn and has 10+INT HP.",
                                               "name":  "Create minion",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "3MP/4HP",
                                               "description":  "An upgrade for Darkhand. A claw of dark power grabs hold of a foe and pins them to the nearest wall, causing 2d4 damage.",
                                               "name":  "Wallmaster",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "2MP/5HP",
                                               "description":  "Touch an enemy in melee range and blast them away with an explosion of dark magic, doing 1d6+int damage and throwing them 3 spaces away.",
                                               "name":  "Dark explosion",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "1MP/5HP",
                                               "description":  "Turn your appearance gruesome. Results are RP related.",
                                               "name":  "Nightmarish form",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "Passive",
                                               "description":  "An upgrade to fog that makes the air painful to breathe, causing INT damage to all those within. Also allows you to cast Dark Fog on others, instead of just yourself. Costs an additional 1MP and 1HP.",
                                               "name":  "Dark fog",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "4MP/4HP",
                                               "description":  "A 2d6+int blast of darkness that does double damage against priests and paladins.",
                                               "name":  "Devoutslayer",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "5MP/3HP",
                                               "description":  "Darkness rains from the sky, doing 1d6 damage to anybody that isn’t fully armored/covered. Lasts until canceled or the cast is knocked out.",
                                               "name":  "Unholy Rain",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "2d6+10 HP",
                                               "description":  "Cause 2d6+10 damage to self in return for a temporary +1 stat bonus for 1 turn.",
                                               "name":  "Self-mutilation",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "2MP",
                                               "description":  "Restore 3d6+5 health and reset your bleed counter, but lose 1 INT and SPI for the remainder of the day. Where does it even go?",
                                               "name":  "Restoration?",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "3MP/8HP",
                                               "description":  "A flow of magic-infused bile pours from your mouth, creating a small blast marker puddle in front of you that does 2d6 damage and reduces armor for those within by 10.",
                                               "name":  "Dark vomit",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "Passive",
                                               "description":  "You permanently lose 1MP, but your melee strikes will inflict +1 darkness damage per hit. Also, it will forever physically hurt people to touch you.",
                                               "name":  "Corrupting touch",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "15HP",
                                               "description":  "An upgrade for the minion that increases health to 20+INT and allows it to shoot 1d3 shots of dark magic that do 1d6 damage each.",
                                               "name":  "Unholy minion",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "5MP/8HP",
                                               "description":  "A 50% chance to possess the body and will of a low level enemy or person.",
                                               "name":  "Puppetry",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "5MP/6HP",
                                               "description":  "A single-target 3d8+int damaging spell.",
                                               "name":  "Crushing Darkness",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "6MP/10HP",
                                               "description":  "Fire 2d20 projectiles that do 1 damage each. Distribute damage between visible targets.",
                                               "name":  "Dark barrage",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "8MP/10HP",
                                               "description":  "The caster creates a temporary portal to any previously visited location, which stands for just 1 turn before collapsing. Roll a 1d10 to determine accuracy of intended arrival point.",
                                               "name":  "Portal",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "Passive skill",
                                               "description":  "A mysterious force whispers enticing secrets. Once per day, sacrifice 1 stat point for the day to gain their unholy guidance.",
                                               "name":  "Whispers from the Dark",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "6MP/8HP",
                                               "description":  "A small blast explosion of darkness that does 2d10+INT damage and knocks foes away in random directions.",
                                               "name":  "Dark eruption",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "Passive",
                                               "description":  "Receive the option to sign a permanent pact. Permanently halves your HP, but gain a powerful, dark creature that attaches itself to your body. It has 6 melee attacks at your finesse, which do 5 dark damage.",
                                               "name":  "Soulseller",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "Passive",
                                               "description":  "Upon near-death, choose to surrender your full self to the power of dark magic. Your conscience may fade, but rest assured that your body will be more powerful than ever before. Make the living pay for what they have done.",
                                               "name":  "Final spell",
                                               "tier":  5
                                           }
                                       ]
                        },
    "Magic: Enhancement":  {
                               "type":  "magic",
                               "name":  "Magic: Enhancement",
                               "displayName":  "Enhancement Magic",
                               "aliases":  [
                                               "Magic: Enhancement",
                                               "Enhancement"
                                           ],
                               "spells":  [
                                              {
                                                  "cost":  "1MP",
                                                  "description":  "Provides fragile objects a protective coating, protecting it from breaking. Can take 5 damage.",
                                                  "name":  "Unbreakable",
                                                  "tier":  1
                                              },
                                              {
                                                  "cost":  "1MP",
                                                  "description":  "Gives an unarmored target a +5 armor. Effect is doubled when the target has natural armor.",
                                                  "name":  "Barkskin",
                                                  "tier":  1
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "A slight touch of wind magic has everyone in the party falling slowly, preventing damage.",
                                                  "name":  "Slowfall",
                                                  "tier":  1
                                              },
                                              {
                                                  "cost":  "1MP",
                                                  "description":  "Renders an ordinarily solid object flexible, allowing easy bending.",
                                                  "name":  "Desolidify",
                                                  "tier":  1
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "Adds +1 damage to bladed weapons for 1 turn.",
                                                  "name":  "Sharpen",
                                                  "tier":  1
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "Grants +1 magic damage of choice to any weapon.",
                                                  "name":  "Elemental touch",
                                                  "tier":  2
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Gives the party +1 movement and silent movement for 1 turn.",
                                                  "name":  "Bunny’s Step",
                                                  "tier":  2
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "Grant a golden glow to any metal armor, piercing darkness and granting +2 CHA.",
                                                  "name":  "Aura of Radiance",
                                                  "tier":  2
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "A person’s clothes gradually heal them, applying 1d4 health per turn.",
                                                  "name":  "Healing infusion",
                                                  "tier":  2
                                              },
                                              {
                                                  "cost":  "2MP",
                                                  "description":  "Renders an object see through.",
                                                  "name":  "Transparency",
                                                  "tier":  2
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Grants 10 magic armor to the target.",
                                                  "name":  "Diffusive ward",
                                                  "tier":  3
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Grants a weapon +1 dark damage per hit. Those vulnerable to darkness take double damage, but it also risks healing those aligned with it.",
                                                  "name":  "Dark Arm Blade",
                                                  "tier":  3
                                              },
                                              {
                                                  "cost":  "4MP",
                                                  "description":  "Party’s weapons are all hidden.",
                                                  "name":  "Chameleon",
                                                  "tier":  3
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Melee weapons emit force projectiles, inflicting weapon damage at range.",
                                                  "name":  "Force weapon",
                                                  "tier":  3
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Grants the party increased jump height and slow fall.",
                                                  "name":  "Winged boots",
                                                  "tier":  3
                                              },
                                              {
                                                  "cost":  "5MP",
                                                  "description":  "Increases target’s size, granting them +3 strength. Also makes them a far easier target, providing everyone a +3 finesse against them.",
                                                  "name":  "Memory of the Giants",
                                                  "tier":  4
                                              },
                                              {
                                                  "cost":  "4MP",
                                                  "description":  "Any arrows fired by a bow this turn multiply, causing triple their usual damage but becomes incapable of critical hits.",
                                                  "name":  "Kriemhild’s arrows",
                                                  "tier":  4
                                              },
                                              {
                                                  "cost":  "6MP",
                                                  "description":  "Grants 15 armor to the target.",
                                                  "name":  "Steelskin",
                                                  "tier":  4
                                              },
                                              {
                                                  "cost":  "5 MP",
                                                  "description":  "Grants an additional attack to any melee weapon wielder or +2 shots to a ranged weapon. Does not affect guns.",
                                                  "name":  "Swiftblade",
                                                  "tier":  4
                                              },
                                              {
                                                  "cost":  "3MP",
                                                  "description":  "Any stick or otherwise straight object can be transformed into a basic weapon for a day.",
                                                  "name":  "Honey’s Armaments",
                                                  "tier":  4
                                              },
                                              {
                                                  "cost":  "5MP",
                                                  "description":  "Inflict damage to the target equal to half their armor value.",
                                                  "name":  "Heat metal",
                                                  "tier":  5
                                              },
                                              {
                                                  "cost":  "8MP",
                                                  "description":  "Every bullet is turned into an area-of-effect explosive.",
                                                  "name":  "Leonard’s Demand",
                                                  "tier":  5
                                              },
                                              {
                                                  "cost":  "4MP",
                                                  "description":  "Shrink a willing target down to miniature size, reducing finesse against them by 5, but rendering them comically weak. Weapons do half damage and strength is set to 1.",
                                                  "name":  "Fairykin",
                                                  "tier":  5
                                              },
                                              {
                                                  "cost":  "8MP",
                                                  "description":  "An upgrade of Steelskin that grants 25 armor and permits the user to turn into liquid form. Liquid form allows passage through narrow openings and detaching from combat without incurring attacks of opportunity.",
                                                  "name":  "Rosailla’s Art",
                                                  "tier":  5
                                              },
                                              {
                                                  "cost":  "6MP",
                                                  "description":  "Makes a target invulnerable to damage and being displaced by magical effects for 1 turn, but also renders them unable to counter-attack.",
                                                  "name":  "Unyielding defender",
                                                  "tier":  5
                                              }
                                          ]
                           },
    "Magic: Force":  {
                         "type":  "magic",
                         "name":  "Magic: Force",
                         "displayName":  "Force Magic",
                         "aliases":  [
                                         "Magic: Force",
                                         "Force"
                                     ],
                         "spells":  [
                                        {
                                            "cost":  "Passive",
                                            "description":  "Gain additional 1 movement speed.",
                                            "name":  "Reduce Resistance",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Target 3 squares in front of you. Blast foes 1d6+2 tiles away and inflict INT damage. Collisions with scenery or other creatures inflict a further 1d6 damage to both.",
                                            "name":  "Force Blast",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1MP",
                                            "description":  "Modify the length and height of the target’s jump by a number of meters up to a total matching your INT.",
                                            "name":  "Great Leap",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "1MP",
                                            "description":  "Pull an object or person towards you for 4+1d6 tiles. Any collisions will damage the target for 2d6 damage.",
                                            "name":  "Forceful Pull",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Int versus str roll. If you win, target is forced to go prone and suffers 1d8+INT damage. They can not act or move in their next turn.",
                                            "name":  "Intense weight",
                                            "tier":  1
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Channel to use a melee weapon at any range, using your INT as the STR modifier for attacks.",
                                            "name":  "Remote control",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "All targets ahead of you are moved 1 tile and suffer 2d4+half-INT damage.",
                                            "name":  "Force Wave",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Roll for INT in an attempt to break any object within touch range. Difficulty is determined by material (i.e. wooden wall or stone).",
                                            "name":  "BREAK",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "Passive",
                                            "description":  "When using throwing weapons or Thrown Weapon, replace their stat modifiers with your INT.",
                                            "name":  "Force Thrower",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "Roll for finesse. Punch an opponent for 2+2d6+INT damage. Any additional modifiers that affect your melee attacks will apply to Kinetric Strike as well.",
                                            "name":  "Kinetic strike",
                                            "tier":  2
                                        },
                                        {
                                            "cost":  "1MP",
                                            "description":  "Channel to raise yourself or a target several centimeters above the ground. Negating the effects of any terrain modifiers. Can be cast and maintained on multiple targets at once.",
                                            "name":  "Hover",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Roll a D6 and add that number to your usual move distance. If this allows you to reach an enemy, attack them with +2 to your strength.",
                                            "name":  "Magical charge",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "Passive",
                                            "description":  "Use your INT to determine what you can carry or wield as opposed to strength.",
                                            "name":  "Weightless",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "4MP",
                                            "description":  "Use an enemy’s armor to crush them. Per 10 armor, roll 1d10+int for damage.",
                                            "name":  "Remote crush",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "2MP",
                                            "description":  "Grant yourself or a target 1 additional attack.",
                                            "name":  "Swiftness",
                                            "tier":  3
                                        },
                                        {
                                            "cost":  "Passive",
                                            "description":  "Gain 2 additional movement.",
                                            "name":  "Superspeed",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "Grant yourself the ability to fly freely.",
                                            "name":  "Flight",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "Use Force Blast on all 8 tiles around you and increase its damage dice to 1d10s.",
                                            "name":  "Improved Force Blast",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "charge - 3MP",
                                            "description":  "Upgrade on Magical Charge. If your rolled result is enough to reach another enemy, move towards them and perform your full physical attacks again.",
                                            "name":  "Chain",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "Roll for INT to lift or move any object in any direction. Large objects like houses may crumble or break as a result.",
                                            "name":  "Force Telekenises",
                                            "tier":  4
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "Any targets within 5 tiles from you lose 1 movement speed and 1 attack.",
                                            "name":  "Oppress",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "5MP",
                                            "description":  "An upgrade over intense weight, inflicting 2d10+int damage.",
                                            "name":  "Crushing weight",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "6MP",
                                            "description":  "Select a point within view of you. Everything within 10 tiles is pulled towards it 1d6 tiles. Those within 1-3 tiles suffer 3d8 damage. 4-6 tiles away 3d6. Others within range receive 3d4 damage.",
                                            "name":  "Point of Gravity",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "3MP",
                                            "description":  "You can not be hit by any ranged attacks coming from the direction you are facing.",
                                            "name":  "Perfect barrier",
                                            "tier":  5
                                        },
                                        {
                                            "cost":  "8MP",
                                            "description":  "Roll for finesse. Transport a target within touch range to a height equal to what you could reach with Great Leap, before using Force to throw them back down. Inflicts 1d10+5*height damage.",
                                            "name":  "Great fall",
                                            "tier":  5
                                        }
                                    ]
                     },
    "Magic: Illusion":  {
                            "type":  "magic",
                            "name":  "Magic: Illusion",
                            "displayName":  "Illusion Magic",
                            "aliases":  [
                                            "Magic: Illusion",
                                            "Illusion"
                                        ],
                            "spells":  [
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Roll for INT. If you succeed, the target is charmed and you can persuade or barter with them as if you have 2 additional charisma for an hour.",
                                               "name":  "Charm",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Roll for INT. If you succeed, the target is agitated and prone to becoming upset or aggressive.",
                                               "name":  "Anger",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "1 MP",
                                               "description":  "Create a small orb of magical light. Allows you and others to see in dark areas.",
                                               "name":  "Illuminate",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "A Magician’s trick where an item within your reach immediately vanishes into your inventory, without anybody being able to see where it went. Do note that this doesn’t clear you from any suspicion when the theft is detected.",
                                               "name":  "Magician’s Sleight",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "1 MP",
                                               "description":  "A single target is made to think that they are being bothered by a small pest, like a cockroach, fly, or wasp, and will act accordingly. Lasts 2 turns. This spell mostly shines outside of combat, because the caster is more important target to kill than some small nuisance.",
                                               "name":  "Minor pest",
                                               "tier":  1
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Roll for INT. If you succeed, the target can be made to remember something simple they wanted to do, like go to the toilet or pay for a drink.",
                                               "name":  "Suggestion",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Target is made to feel that their sight is blurred, reducing finesse by 1.",
                                               "name":  "Blur",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Create a temporary copy of an item currently in your sight. Disappears after an hour.",
                                               "name":  "Arei’s Mirror",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Induce a headache in the target, inflicting 2d3+INT damage.",
                                               "name":  "Headache",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Sooth a target’s mind, curing them from any fear effects.",
                                               "name":  "Happy thoughts",
                                               "tier":  2
                                           },
                                           {
                                               "cost":  "4 MP",
                                               "description":  "Create a clone of yourself which can be directed to go anywhere, but dissipates when touched by another person. Can’t cast spells or interact with anything.",
                                               "name":  "Controlled Afterimage",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Darken an area, removing natural light and making it hard to see.",
                                               "name":  "Remove light",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Cause a sound somewhere in the audible distance that resembles the sound of running or something falling over.",
                                               "name":  "Phantom footstep",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "2 MP",
                                               "description":  "Any weapon can be made invisible to onlookers or made to look like something innocuous, like a bouquet of flowers.",
                                               "name":  "Obfuscate",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "4 MP",
                                               "description":  "Target is overwhelmed by sudden hallucinations, reducing their stats by 1 for 1 turn.",
                                               "name":  "Weird",
                                               "tier":  3
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Inflict 2d6+5 direct damage.",
                                               "name":  "Phantom Pain",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "5 MP",
                                               "description":  "Make yourself look like someone else or an object. If the latter, then moving dispels the illusion.",
                                               "name":  "Shapeshift",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Onlookers become unable to see an alcove or path, treating it as if there is a wall there similar to any adjacent walls. People that were previously able to see the path will see the wall form, but realize that it’s fake.",
                                               "name":  "Hide path",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "4 MP",
                                               "description":  "Become completely invisible, though reality around you ripples whenever you touch an object or perform a special action. If somebody touches you, invisibility is cancelled. Anybody you hold hands with becomes invisible too.",
                                               "name":  "Invisibility",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "5 MP",
                                               "description":  "An object is made to look like it’s alive. Balls will roll off on their own, furniture will walk away. After 10 minutes, the object will return to normal.",
                                               "name":  "Animate Magic",
                                               "tier":  4
                                           },
                                           {
                                               "cost":  "5 MP",
                                               "description":  "Roll for INT. If you succeed, the target is briefly convinced that they have a crush on the caster. This drastically reduces the difficulty of persuasion rolls. This can’t be used against targets that are not romantically viable to you.",
                                               "name":  "Seduce",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "Passive",
                                               "description":  "ability. Your movement becomes distorted, giving any foe a permanent -2 finesse against you.",
                                               "name":  "Shapeless",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "5MP",
                                               "description":  "Deal 2D8+4+INT psychic damage to a target.",
                                               "name":  "Psychic shock",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "3 MP",
                                               "description":  "Make it appear as if you died, suppressing all your vital life signs for up to 10 minutes.",
                                               "name":  "Feign death",
                                               "tier":  5
                                           },
                                           {
                                               "cost":  "5 MP",
                                               "description":  "Roll for INT. If you succeed, you can make a target say any 1 sentence in their own voice.",
                                               "name":  "Ventriloquism",
                                               "tier":  5
                                           }
                                       ]
                        },
    "Magic: Nature":  {
                          "type":  "magic",
                          "name":  "Magic: Nature",
                          "displayName":  "Nature Magic",
                          "aliases":  [
                                          "Magic: Nature",
                                          "Nature"
                                      ],
                          "spells":  [
                                         {
                                             "cost":  "1MP",
                                             "description":  "All plant life under a small blast marker is immediately brought to full bloom.",
                                             "name":  "Growth Spurt",
                                             "tier":  1
                                         },
                                         {
                                             "cost":  "1MP",
                                             "description":  "A root bursts from the ground to strike a foe, causing 1d4+2 damage. May be used multiple times per turn. Builds a bleed counter.",
                                             "name":  "Rootlash",
                                             "tier":  1
                                         },
                                         {
                                             "cost":  "1MP",
                                             "description":  "Sets a trap on a tile. When a person crosses it, a bouncy mushroom springs from the ground, launching them upwards for 2+1d6 meters. Upon landing they take damage equal to the 1d6 roll.",
                                             "name":  "Bounceshroom",
                                             "tier":  1
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "Restores 2d3+int health to the target.",
                                             "name":  "Minor heal",
                                             "tier":  1
                                         },
                                         {
                                             "cost":  "Passive",
                                             "description":  "Anyone with Tier 1 of this school can receive Nature’s Bounty for performing deeds that benefit nature. These can be traded in at the archdruid or her disciples for rewards.",
                                             "name":  "Nature’s Bounty",
                                             "tier":  1
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "Brings someone back from the dead, so long as they only passed away within the past hour. They will have 1HP and 0MP, but all mortal wounds are healed.",
                                             "name":  "Emergency restore",
                                             "tier":  2
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "Target is healed for 10% of their max health per turn, but a mushroom appears on their head. Only 1 target can have the mushroom at any time.",
                                             "name":  "Healthshroom",
                                             "tier":  2
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "A thorny wreath wraps around the target’s head, tightly digging into their scalp. Inflicts 1d4+int bleeding damage every turn. If the target has a helmet, the crown forms underneath it.",
                                             "name":  "Crown of thorns",
                                             "tier":  2
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "Creates little plants to absorb the poison from the air. If no air is present, these plants generate breathable air.",
                                             "name":  "Purify air",
                                             "tier":  2
                                         },
                                         {
                                             "cost":  "1MP",
                                             "description":  "Vines and roots appear to lock a target in place, requiring them to make a strength check at -2 to escape.",
                                             "name":  "Entanglement",
                                             "tier":  2
                                         },
                                         {
                                             "cost":  "4MP",
                                             "description":  "A projectile strikes the target and then explodes into a shower of leaves, causing 2d8+int damage and reducing finesse by 2 for 2 turns.",
                                             "name":  "Seras’ wrath",
                                             "tier":  3
                                         },
                                         {
                                             "cost":  "3MP",
                                             "description":  "Restore 2d6+3+INT health to a target.",
                                             "name":  "Heal",
                                             "tier":  3
                                         },
                                         {
                                             "cost":  "Passive",
                                             "description":  "The smell of nature soothes animals, rendering them passive. They won’t attack you.",
                                             "name":  "Animal Scent",
                                             "tier":  3
                                         },
                                         {
                                             "cost":  "2MP",
                                             "description":  "Removes all harmful effects on the target.",
                                             "name":  "Tranquility",
                                             "tier":  3
                                         },
                                         {
                                             "cost":  "3MP",
                                             "description":  "A cloud of noxious fumes from the great swamp permeate the air, causing 1d4+int choking damage to all on the field.",
                                             "name":  "Swampfumes",
                                             "tier":  3
                                         },
                                         {
                                             "cost":  "4MP",
                                             "description":  "Entanglement now causes 2d6 damage every turn whilst active.",
                                             "name":  "Improve entanglement",
                                             "tier":  4
                                         },
                                         {
                                             "cost":  "4MP",
                                             "description":  "Summons a stationary Tree Warrior to guard its post. Has 30health and 15 armor, and performs 4 attacks every turn. These inflict 2 damage +6 strength.",
                                             "name":  "Guardian Tree",
                                             "tier":  4
                                         },
                                         {
                                             "cost":  "3MP",
                                             "description":  "Frees a target from any rot or corruption possessing them.",
                                             "name":  "Great Purification",
                                             "tier":  4
                                         },
                                         {
                                             "cost":  "3MP",
                                             "description":  "Gives yourself elegant wings, allowing slow flight.",
                                             "name":  "Butterfly’s Boon",
                                             "tier":  4
                                         },
                                         {
                                             "cost":  "4MP",
                                             "description":  "A cloud of petals fills the air, healing all under a small blast marker for 2d6+INT.",
                                             "name":  "Healing petals",
                                             "tier":  4
                                         },
                                         {
                                             "cost":  "5MP",
                                             "description":  "Returns a fallen person back to life at half health.",
                                             "name":  "Touch of life",
                                             "tier":  5
                                         },
                                         {
                                             "cost":  "10MP",
                                             "description":  "What is given may also be taken. A forbidden druidic art that kills a mortal person so long as their skin can be touched. Finesse roll is required.",
                                             "name":  "Touch of unlife",
                                             "tier":  5
                                         },
                                         {
                                             "cost":  "5MP",
                                             "description":  "Mushroom appears on the target, inflicting 2d6+int damage per turn and restoring it to the druid.",
                                             "name":  "Drainingshroom",
                                             "tier":  5
                                         },
                                         {
                                             "cost":  "5MP",
                                             "description":  "Guardian Tree can now move and heals itself for 5HP every turn.",
                                             "name":  "Improved Guardian Tree",
                                             "tier":  5
                                         },
                                         {
                                             "cost":  "5MP",
                                             "description":  "Heals target for 4d6+5+INT.",
                                             "name":  "Great Heal",
                                             "tier":  5
                                         }
                                     ]
                      },
    "Magic: Necromancy":  {
                              "type":  "magic",
                              "name":  "Magic: Necromancy",
                              "displayName":  "Necromancy Magic",
                              "aliases":  [
                                              "Magic: Necromancy",
                                              "Necromancy"
                                          ],
                              "spells":  [
                                             {
                                                 "cost":  "Passive",
                                                 "description":  "Gain the ability to speak with ghosts, including calling forth the spirits of the recently-deceased from their corpse.",
                                                 "name":  "Speak to dead",
                                                 "tier":  1
                                             },
                                             {
                                                 "cost":  "2MP",
                                                 "description":  "Heal an undead for 2d6 health.",
                                                 "name":  "Restore undead",
                                                 "tier":  1
                                             },
                                             {
                                                 "cost":  "1MP",
                                                 "description":  "Create a flying sentry from a dormant skull. No attack capacity, but can distract, pick up and transport items, and scout for the necromancer.",
                                                 "name":  "Winged skull",
                                                 "tier":  1
                                             },
                                             {
                                                 "cost":  "2MP",
                                                 "description":  "An unseen ghost protects you from a single blow in the next enemy round.",
                                                 "name":  "Spectral guardian",
                                                 "tier":  1
                                             },
                                             {
                                                 "cost":  "2MP",
                                                 "description":  "A ranged projectile that inflicts 2d6 physical damage.",
                                                 "name":  "Bone spike",
                                                 "tier":  1
                                             },
                                             {
                                                 "cost":  "Passive",
                                                 "description":  "Turn a singular corpse into your minion, creating a basic zombie with 4 strength, 2 attacks, +7 to hit, and 20HP. 1MP to maintain.",
                                                 "name":  "Raise dead",
                                                 "tier":  2
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Nurture yourself on the energy of a nearby ghost, healing yourself for 1d8+int health.",
                                                 "name":  "Consume soul",
                                                 "tier":  2
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Create a gaseous cloud reeking of rotten flesh. Inflicts 2d4 damage to all within and reduces their finesse by 2.",
                                                 "name":  "Corpse fume",
                                                 "tier":  2
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Bony arms rise from the ground, gripping foes by the ankle. Roots everyone in a small blast area for 1 turn.",
                                                 "name":  "Skeletal hands",
                                                 "tier":  2
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Inflict 3d3+int in cold damage to a single target.",
                                                 "name":  "Spinechiller",
                                                 "tier":  2
                                             },
                                             {
                                                 "cost":  "4MP",
                                                 "description":  "Cause a corpse to explode, inflicting 2d6+int damage to adjacent targets.",
                                                 "name":  "Corpse explosion",
                                                 "tier":  3
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Provide yourself with +10 physical armor.",
                                                 "name":  "Bone shield",
                                                 "tier":  3
                                             },
                                             {
                                                 "cost":  "Passive",
                                                 "description":  "Create a skeleton with a weapon of your choice and 30HP, +6 to hit. 2MP to maintain.",
                                                 "name":  "Raise skeleton",
                                                 "tier":  3
                                             },
                                             {
                                                 "cost":  "Passive",
                                                 "description":  "Lifts the restriction for just 1 zombie. Have as many as your MP allows.",
                                                 "name":  "Summon the horde",
                                                 "tier":  3
                                             },
                                             {
                                                 "cost":  "2MP",
                                                 "description":  "The light suits ye poorly. Consume all fire and magical light sources in range.",
                                                 "name":  "Darken",
                                                 "tier":  3
                                             },
                                             {
                                                 "cost":  "3MP",
                                                 "description":  "Call forth a skeletal scythe, which has 3 attacks, 8 magical damage, and double damage against undead.",
                                                 "name":  "Reaper’s reaper",
                                                 "tier":  4
                                             },
                                             {
                                                 "cost":  "4MP",
                                                 "description":  "Break a bone within the target’s body, incapacitating them and inflicting 2d8+5 unsaveable damage.",
                                                 "name":  "Bonesnapper",
                                                 "tier":  4
                                             },
                                             {
                                                 "cost":  "5MP",
                                                 "description":  "3d6+int damage against a single target.",
                                                 "name":  "Dissolve flesh",
                                                 "tier":  4
                                             },
                                             {
                                                 "cost":  "5MP",
                                                 "description":  "Everyone in a radius around you suffers 1d6+int unsaveable damage as migraines and aches overwhelm their body.",
                                                 "name":  "Wave of pain",
                                                 "tier":  4
                                             },
                                             {
                                                 "cost":  "5MP",
                                                 "description":  "Heal an undead for 3d8+int+5",
                                                 "name":  "Improved unheal",
                                                 "tier":  4
                                             },
                                             {
                                                 "cost":  "8MP",
                                                 "description":  "Tie your life to a valuable object. If you die, you return to (un)life after 3 turns. Unless the object is destroyed.",
                                                 "name":  "Minor lichdom",
                                                 "tier":  5
                                             },
                                             {
                                                 "cost":  "4MP",
                                                 "description":  "Consume any non-necromancy buff and defile it. Doubling its potency.",
                                                 "name":  "Defile",
                                                 "tier":  5
                                             },
                                             {
                                                 "cost":  "9MP",
                                                 "description":  "Cause corpse explosion on every corpse currently in your sight.",
                                                 "name":  "Mass Corpse explosion",
                                                 "tier":  5
                                             },
                                             {
                                                 "cost":  "Passive",
                                                 "description":  "Animate the corpse of any monster. Costs 5MP to maintain.",
                                                 "name":  "Raise abomination",
                                                 "tier":  5
                                             },
                                             {
                                                 "cost":  "9MP",
                                                 "description":  "Inflict 2d10+int+10 damage to a single target. If this kills them, their skull is turned into a winged skull.",
                                                 "name":  "Seize skull",
                                                 "tier":  5
                                             }
                                         ]
                          },
    "Magic: Nightmare":  {
                             "type":  "magic",
                             "name":  "Magic: Nightmare",
                             "displayName":  "Nightmare Magic",
                             "aliases":  [
                                             "Magic: Nightmare",
                                             "Nightmare"
                                         ],
                             "spells":  [
                                            {
                                                "cost":  "2 MP",
                                                "description":  "Make a target believe that maggots crawl underneath their skin, causing them to excessively scratch at their body. Causes 2d3+2 self-inflicted damage.",
                                                "name":  "Rena’s Mania",
                                                "tier":  1
                                            },
                                            {
                                                "cost":  "2 MP",
                                                "description":  "Roll a 1d6 to learn a random, personal detail about the target.",
                                                "name":  "Steal memory",
                                                "tier":  1
                                            },
                                            {
                                                "cost":  "4 MP",
                                                "description":  "Roll for INT. If you succeed, the target becomes hysteric and begins acting in utter panic.",
                                                "name":  "Hysteria",
                                                "tier":  1
                                            },
                                            {
                                                "cost":  "2 MP",
                                                "description":  "An ear-piercing noise inflicts INT damage to all targets under a small blast marker.",
                                                "name":  "Harrowing scream",
                                                "tier":  1
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Roll for INT. If you succeed, the target is overwhelmed by sudden sadness and listlessness. Lasts for an hour before the thoughts are shaken off.",
                                                "name":  "Depress",
                                                "tier":  1
                                            },
                                            {
                                                "cost":  "4 MP",
                                                "description":  "Cause the target nightmares that affect their sleep, tiring them out in subsequent days for up to 2 weeks.",
                                                "name":  "Restlessness",
                                                "tier":  2
                                            },
                                            {
                                                "cost":  "Passive",
                                                "description":  "Learn to communicate in a horrific tongue known only to other adepts at nightmare magic. Will sound demonic to any onlookers, though Witch Hunters are able to recognize the speech and will act accordingly.",
                                                "name":  "Adept’s Tongue",
                                                "tier":  2
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Target is afflicted by a terrible memory from their past just as they launch an attack, halving the total amount of attack they are capable of making.",
                                                "name":  "Horrific memory",
                                                "tier":  2
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Everybody is subject to an obnoxious drone in a radius around you, which gives a 30% chance to disrupt any miracles from being casted and is, generally, fucking annoying.",
                                                "name":  "Ambient drone",
                                                "tier":  2
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Cause 2d4+INT damage to a single target.",
                                                "name":  "Mindflay",
                                                "tier":  2
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Once per event, Increases the difficulty of any skill checks for professions or spell effects for everyone in the vicinity by 5.",
                                                "name":  "Terror radius",
                                                "tier":  3
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Transforms your arm into a fleshy, extending appendage. Grab a single target for 2d6 physical damage and either pull them towards you or move them to a spot as many squares away from you.",
                                                "name":  "Saya’s Grasp",
                                                "tier":  3
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Roll INT against target’s SPI. If you win, they can no longer tell friend from foe. Attacking whoever is closest to them.",
                                                "name":  "Blind Hatred",
                                                "tier":  3
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "You follow a target as if looking over their shoulder at all times. They feel watched and believe they see eyes around them, making them wary or frightened. If they know about this spell, they will attempt to find the caster.",
                                                "name":  "The Eyes",
                                                "tier":  3
                                            },
                                            {
                                                "cost":  "2 MP",
                                                "description":  "Roll INT and against the target’s SPI. If you win, they are frightened for 1 turn and will run away to hide. If they are in melee combat, they will trigger attacks of opportunity and then flee in the direction with the least enemies.",
                                                "name":  "Fear",
                                                "tier":  3
                                            },
                                            {
                                                "cost":  "5 MP",
                                                "description":  "A threatening creature is made to appear, causing people to act as if they have spotted a monster. In combat, the creature counts as if it’s casting taunt, but will disappear after the first hit.",
                                                "name":  "Manifest creature",
                                                "tier":  4
                                            },
                                            {
                                                "cost":  "5 MP",
                                                "description":  "Roll INT versus the target’s SPI. If you win, you can implant a thought into their dreams that they come to believe more intensely over the course of some time. They gain a bonus on SPI if the thought you are trying to force on them opposes their ideals.",
                                                "name":  "Corrupting dream",
                                                "tier":  4
                                            },
                                            {
                                                "cost":  "5 MP",
                                                "description":  "Realizations about the horrors of this world saps the faithful of their piety. Blocks use of bibles entirely and reduces spirit by 4 on a single target",
                                                "name":  "Faithlessness",
                                                "tier":  4
                                            },
                                            {
                                                "cost":  "4 MP",
                                                "description":  "Make enemies under a large blast marker believe they are in a frightening, alternate world. They walk with extra caution to avoid imagined hazards, reducing movement speed by 1.",
                                                "name":  "Hellworld",
                                                "tier":  4
                                            },
                                            {
                                                "cost":  "4 MP",
                                                "description":  "Make a target forget their most familiar language, leaving them unable to communicate or read in it.",
                                                "name":  "Forget language",
                                                "tier":  4
                                            },
                                            {
                                                "cost":  "3 MP",
                                                "description":  "Exchange 1 END for the day to add 2 temporary stat points to another stat for 2 rounds. If you were at full health, doing this consumes 10HP.",
                                                "name":  "No Rest for the Wicked",
                                                "tier":  5
                                            },
                                            {
                                                "cost":  "5MP",
                                                "description":  "3d10+int psychic blast to the enemy with a 50% chance to either stun or enrage them.",
                                                "name":  "Mindbreak",
                                                "tier":  5
                                            },
                                            {
                                                "cost":  "5 MP",
                                                "description":  "Roll INT versus SPI against the target. If they lose, they fall asleep until making a roll of 25 (minus SPI) or if woken up by someone else. On the first turn they take 1 psychic damage, which is doubled for every subsequent turn spent asleep.",
                                                "name":  "Profane Slumber",
                                                "tier":  5
                                            },
                                            {
                                                "cost":  "Passive",
                                                "description":  "Once per event, sacrifice all mana and HP to fade into a dreamworld for 1 hour. This is comparable to an invincibility spell, except you can freely pass through walls and objects while the spell is active. When you emerge, HP and Mana are drained until recovered. Don’t look up.",
                                                "name":  "Dreamstate",
                                                "tier":  5
                                            },
                                            {
                                                "cost":  "5 MP",
                                                "description":  "Roll INT versus SPI, unless the target doesn’t care to resist. Damage from spells is doubled and +10 STR bonus to attack rolls for 1 day. However, the target becomes a raving lunatic, lashing out at any except their closest companions. Each turn they must roll a 9 or 10, or will be unable to speak coherently and random issues may occur. Spell can be cast on self.",
                                                "name":  "Inflict Insanity",
                                                "tier":  5
                                            }
                                        ]
                         },
    "Bible of Felis":  {
                           "type":  "faith",
                           "name":  "Bible of Felis",
                           "displayName":  "Bible of Felis",
                           "deity":  "Felis (Goddess of Nature \u0026 Beasts)",
                           "aliases":  [
                                           "Bible of Felis",
                                           "Felis"
                                       ],
                           "prayers":  [
                                           {
                                               "percentage":  "10%",
                                               "name":  "Soothe Nature",
                                               "quote":  "Those who venture with Felis, venture among friends.",
                                               "effect":  "At the cost of 1SP, make any animal or flora-based enemy passive to you for 2 turns. Breaks when the target is attacked again."
                                           },
                                           {
                                               "percentage":  "20%",
                                               "name":  "Shared Power",
                                               "quote":  "All the wonders of the world were hers, yet she chose to share them.",
                                               "effect":  "At the cost of 1SP, duplicate a buff effect to another ally."
                                           },
                                           {
                                               "percentage":  "30%",
                                               "name":  "Lush",
                                               "quote":  "And all she touched came to life, and it was beautiful.",
                                               "effect":  "At the cost of 1SP, spawn all manner of flora around you appropriate to current terrain. All targets within gain an extra +5 to any heals affecting them."
                                           },
                                           {
                                               "percentage":  "40%",
                                               "name":  "Heal and heal again",
                                               "quote":  "When nature is at stake, the very rules of magic may bend.",
                                               "effect":  "In return for 1SP, the effect of a healing spell may be doubled or the dice rolls be rerolled if preferable."
                                           },
                                           {
                                               "percentage":  "50%",
                                               "name":  "Cure all",
                                               "quote":  "Neither her power nor her kindness knew limits.",
                                               "effect":  "For 2SP, dispel 1 negative effect on all party members or all negative effects on a single target."
                                           },
                                           {
                                               "percentage":  "60%",
                                               "name":  "Undying guardian",
                                               "quote":  "So long as our friends persist, so does our duty.",
                                               "effect":  "After being knocked out or killed, the worshipper can persist as a spirit of healing. Continuing to cast any healing spell or prayer albeit with halved stats. Spells may not target oneself."
                                           },
                                           {
                                               "percentage":  "70%",
                                               "name":  "Druidic Nemesis",
                                               "quote":  "No greater betrayal than a daughter forsaking her mother.",
                                               "effect":  "For 2SP, all nature magic Bible of Felis spells have their potency doubled against targets aligned with Nesfeth."
                                           },
                                           {
                                               "percentage":  "80%",
                                               "name":  "Nature’s purgation",
                                               "quote":  "Nature’s greatness will be preserved.",
                                               "effect":  "For 2SP, cast a 3d8+SPI blast at a single target that either restores health to friends or inflicts direct damage."
                                           },
                                           {
                                               "percentage":  "90%",
                                               "name":  "Bountiful repayment",
                                               "quote":  "Those who have served nature will be rewarded.",
                                               "effect":  "At the cost of 1 Nature’s Bounty, restore a target to life with full health without expending a spirit or MP. May be triggered on self if so desired."
                                           },
                                           {
                                               "percentage":  "100%",
                                               "name":  "Arise again",
                                               "quote":  "Nature can ill-afford to lose its greatest champions.",
                                               "effect":  "For 3SP, return a friendly target to life with full health, full MP, and they will be fully actionable for that round of turns."
                                           }
                                       ]
                       },
    "Bible of Illumus":  {
                             "type":  "faith",
                             "name":  "Bible of Illumus",
                             "displayName":  "Bible of Illumus",
                             "deity":  "Illumus (God of Light \u0026 Righteousness)",
                             "aliases":  [
                                             "Bible of Illumus",
                                             "Illumus"
                                         ],
                             "prayers":  [
                                             {
                                                 "percentage":  "10%",
                                                 "name":  "Sense evil",
                                                 "quote":  "You may not know its form, but you will know its presence.",
                                                 "effect":  "At the cost of 1SP, sense if any evil entities are near and gain a rough understanding of their nature."
                                             },
                                             {
                                                 "percentage":  "20%",
                                                 "name":  "Righteous Cause",
                                                 "quote":  "Believe and you will succeed.",
                                                 "effect":  "At the cost of 1SP, provide yourself and adjacent allies with SPI+1d10 magic ward."
                                             },
                                             {
                                                 "percentage":  "30%",
                                                 "name":  "Resist Evil",
                                                 "quote":  "There exists no greater sin than to lose oneself to temptation",
                                                 "effect":  "At the cost of 1SP, break out from any magical hold or resist any magical attempt at controlling you."
                                             },
                                             {
                                                 "percentage":  "40%",
                                                 "name":  "Enemies everywhere",
                                                 "quote":  "We are surrounded by evil. You need only see it.",
                                                 "effect":  "For 1SP, regard all entities on the field as evil for your next turn."
                                             },
                                             {
                                                 "percentage":  "50%",
                                                 "name":  "Smite",
                                                 "quote":  "SMITE",
                                                 "effect":  "For 2SP, smite a target in melee range for 2d10+SPI. Diceroll may be retried if target is considered evil."
                                             },
                                             {
                                                 "percentage":  "60%",
                                                 "name":  "Chains of justice",
                                                 "quote":  "Come here, so that I might smite you down.",
                                                 "effect":  "At the cost of 2SP, bind any visible, evil entities with chains inflicting 1d6+SPI damage. Then pull them towards you for (up to) 1d6+2 tiles."
                                             },
                                             {
                                                 "percentage":  "70%",
                                                 "name":  "Words of justice",
                                                 "quote":  "Let them hear your truth and let them fear it.",
                                                 "effect":  "For 2SP, inflict SPI+CHA damage to all evil targets in hearing range. Reduce their strength by 1."
                                             },
                                             {
                                                 "percentage":  "80%",
                                                 "name":  "Aura of Justice",
                                                 "quote":  "They will yearn to be like you.",
                                                 "effect":  "For 3SP, apply a +1 stat boosts for 2 turns to all your allies."
                                             },
                                             {
                                                 "percentage":  "90%",
                                                 "name":  "Angel of Justice",
                                                 "quote":  "You need not await his soldiers, for you are one of them.",
                                                 "effect":  "At the cost of 3 SP, gain fully-functioning magical wings capable of carrying you regardless of armor weight."
                                             },
                                             {
                                                 "percentage":  "100%",
                                                 "name":  "Second Coming",
                                                 "quote":  "Your job remains unfinished.",
                                                 "effect":  "At the cost of 4SP, revive with full health after remaining downed for 1 turn."
                                             }
                                         ]
                         },
    "Bible of Zuul":  {
                          "type":  "faith",
                          "name":  "Bible of Zuul",
                          "displayName":  "Bible of Zuul",
                          "deity":  "Zuul (God of Trickery \u0026 Ambition)",
                          "aliases":  [
                                          "Bible of Zuul",
                                          "Zuul"
                                      ],
                          "prayers":  [
                                          {
                                              "percentage":  "10%",
                                              "name":  "Sense morality",
                                              "quote":  "People are impossible to categorize, but we may certainly try.",
                                              "effect":  "At the cost of 1SP, gain insight on where a person falls on the morality scale."
                                          },
                                          {
                                              "percentage":  "20%",
                                              "name":  "Detect want",
                                              "quote":  "The key to manipulation is knowledge of one’s true desires. Listen to their words and peer into their soul.",
                                              "effect":  "At the cost of 1SP, learn of underlying motives in a conversation partner."
                                          },
                                          {
                                              "percentage":  "30%",
                                              "name":  "Second chance",
                                              "quote":  "One’s fate is set in stone, but even stones can be turned.",
                                              "effect":  "At the cost of 1SP, reroll any dice throw."
                                          },
                                          {
                                              "percentage":  "40%",
                                              "name":  "Force reroll",
                                              "quote":  "Luck and fortune are privileges. Thus it is our right to rescind them, should it serve our needs.",
                                              "effect":  "Like Second Chance, but you force someone else to reroll instead. Costs 1SP"
                                          },
                                          {
                                              "percentage":  "50%",
                                              "name":  "Wild magic gambit",
                                              "quote":  "As the libraries of Zuul burned, understanding of magic was set back a thousand years.",
                                              "effect":  "Roll to cast a random spell from any school. The amount of SP sacrificed determines the tier of the spell."
                                          },
                                          {
                                              "percentage":  "60%",
                                              "name":  "Misfortune teller",
                                              "quote":  "In that instant the veil of destiny was pierced, rendering the future clear.",
                                              "effect":  "Roll a 1d10 to gain insight in a person’s future. The higher the roll, the more detailed the information. You can not gain information that pertains to yourself this way. Yours is a destiny for others to manipulate. Costs 2SP"
                                          },
                                          {
                                              "percentage":  "70%",
                                              "name":  "Steal magic",
                                              "quote":  "The kingdom’s magic may be unrivaled, yet that of others remains worthwhile to be studied.",
                                              "effect":  "At the cost of 2SP, transfer a magical effect on the target to yourself."
                                          },
                                          {
                                              "percentage":  "80%",
                                              "name":  "Traitor’s banishment",
                                              "quote":  "It was a great and wonderful scheme, brought down by one man’s sentimentality.",
                                              "effect":  "Spend 3SP to crush a single target with Zuul’s rage. All magical effects are dispelled and 2d10+10 damage is inflicted."
                                          },
                                          {
                                              "percentage":  "90%",
                                              "name":  "Aura of Zuul",
                                              "quote":  "Years later, areas of the castle where Zuul had resided remained inaccessible to all but the most powerful sorcerers.",
                                              "effect":  "Spend 3SP to bewitch an area the size of a large-blast marker, making it so as if Zuul himself is present. All creatures without magical resistance take 1d6 magic damage per turn, magical devices go haywire, and properties on random objects shift unpredictably. Lasts 1 day."
                                          },
                                          {
                                              "percentage":  "100%",
                                              "name":  "Ethereal shape",
                                              "quote":  "We resented not being cast into this world as Zuul’s chosen children and so we took matters in our own hands.",
                                              "effect":  "At the cost of 1SP per turn, shift into a shape akin to the Ultkor. A swirling mass of sentient magic, capable of phasing through walls and resisting hostile spells. The effect is cancalled immediately when SP drops below 5."
                                          }
                                      ]
                      }
};
