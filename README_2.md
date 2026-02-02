# ScamShield User Guide

## 📋 Table of Contents
1. [How It Works](#how-it-works)
2. [File Structure](#file-structure)
3. [Customizing Detection](#customizing-detection)
4. [Understanding Scores](#understanding-scores)
5. [Adding New Patterns](#adding-new-patterns)
6. [Commands](#commands)
7. [Troubleshooting](#troubleshooting)

---

## 🔍 How It Works

ScamShield uses a **multi-layered detection system** with **automatic scam type discovery** to identify scam attempts in Hypixel chat:

### Auto-Discovery System

**NEW:** ScamShield automatically finds and loads scam types!

- Drop any `scamtype-*.json` file into `packcore/scamshield/`
- Run `/scamshield reload`
- The file is automatically detected and loaded
- No code editing required!

**Example:** Create `scamtype-crypto.json` → Run `/scamshield reload` → Instantly active!

### Detection Layers

1. **Psychological Tactics** (`phishing-language.json`)
    - Detects *HOW* scammers manipulate (urgency, authority, trust-building)
    - Reusable across all scam types
    - Examples: "quick!", "trust me", "i'm staff"

2. **Scam-Specific Actions** (Individual scam type files)
    - Detects *WHAT* scammers ask you to do
    - Unique to each scam type
    - Examples: "/coopadd me", "verify your account", "flex your items"

3. **Conversation Progression** (Built into code)
    - Tracks conversation stages (Initial → Setup → Exploitation)
    - Detects multi-message patterns
    - Identifies escalation and dangerous sequences

4. **Context Awareness** (Built into code)
    - Considers your current activity (dungeons, lobby, etc.)
    - Reduces false positives for legitimate trades
    - Adjusts sensitivity based on situation

### Detection Flow

```
Message Received
    ↓
Normalize & Check Cache
    ↓
Run All Detectors in Parallel:
  • Phishing Language Analyzer
  • Discord Verify Detector
  • Island Theft Detector
  • Trade Manipulation Detector
  • Free Rank Bait Detector
  • Command Instruction Detector
    ↓
Calculate Total Score
    ↓
Check Conversation History
    ↓
Apply Context Multipliers
    ↓
Compare to Threshold (100 points)
    ↓
Trigger Warning if Exceeded
```

---

## 📁 File Structure

All configuration files are in `packcore/scamshield/`:

```
packcore/scamshield/
├── phishing-language.json       # Pure psychological tactics
├── scamtype-discord-verify.json # Discord verification scams
├── scamtype-island-theft.json   # Island/co-op theft scams
├── scamtype-trade-manipulation.json # Trade & auction scams
├── scamtype-free-rank.json      # Free reward bait scams
└── detections.json              # Your detection history (auto-generated)
```

### File Responsibilities

| File | Detects | Example Phrases |
|------|---------|-----------------|
| `phishing-language.json` | Manipulation tactics | "quick!", "trust me", "i'm staff" |
| `discord-verify` | Discord → verification → credentials | "join discord", "verify account", "send code" |
| `island-theft` | Co-op/visit commands + giveaways | "/coopadd", "i'm quitting", "all my items" |
| `trade-manipulation` | Trade scams & auction tricks | "flex your", "pay you 10%", "accidentally put" |
| `free-rank` | Free reward baits | "free rank", "/visit me to claim" |

---

## ⚙️ Customizing Detection

### Adjusting Sensitivity

1. **Lower Detection Threshold** (detect more, but more false positives)
   ```
   /scamshield config
   Set threshold: 80 (default: 100)
   ```

2. **Raise Detection Threshold** (detect less, fewer false positives)
   ```
   Set threshold: 150
   ```

### Whitelisting Friends

If a friend triggers false positives:

```
/scamshield whitelist add PlayerName
```

View whitelisted players:
```
/scamshield whitelist list
```

Remove from whitelist:
```
/scamshield whitelist remove PlayerName
```

### Editing JSON Files

**Location:** `packcore/scamshield/`

**After editing:** Run `/scamshield reload` to apply changes

#### Adding Phrases to Existing Patterns

Open any scam type file and find the pattern group:

```json
{
  "pattern_groups": {
    "verification_request": {
      "description": "Requesting account verification",
      "score": 35,
      "phrases": [
        "verify",
        "verification",
        "verify your account",
        "YOUR NEW PHRASE HERE"  // ← Add here
      ]
    }
  }
}
```

#### Adjusting Scores

**Pattern Group Score:** Base points for detecting this pattern

```json
"verification_request": {
  "score": 35,  // ← Higher = more suspicious
  "phrases": [...]
}
```

**Combination Bonus:** Extra points when multiple patterns match together

```json
"combination_rules": [
  {
    "description": "Discord + Verification = scam",
    "requires": ["discord_invitation", "verification_request"],
    "bonus": 60  // ← Adjust this
  }
]
```

#### Creating New Pattern Groups

Add to the `pattern_groups` object:

```json
"your_new_pattern": {
  "description": "What this detects",
  "score": 25,
  "phrases": [
    "phrase 1",
    "phrase 2",
    "phrase 3"
  ]
}
```

Then add combination rules to link it with other patterns:

```json
"combination_rules": [
  {
    "description": "New pattern + existing pattern",
    "requires": ["your_new_pattern", "existing_pattern"],
    "bonus": 50
  }
]
```

---

## 📊 Understanding Scores

### Score Ranges

| Score | Confidence | Warning Type | Response |
|-------|-----------|--------------|----------|
| 0-99 | None | No warning | Message passes through |
| 100-149 | LOW | Gentle warning | Yellow text, suggest whitelist |
| 150-249 | MEDIUM | Strong warning | Orange text, education link |
| 250+ | HIGH | Critical warning | Red text, force warning screen |

### How Scores Are Calculated

**Base Score** = Sum of all matching patterns

**Example:**
```
Message: "quick! join my discord to verify your account"

Phishing Language:
  • urgency ("quick"): +15
  
Discord Verify:
  • discord_invitation: +20
  • verification_request: +35
  
Combination Bonuses:
  • Discord + Verification: +60

Total: 130 points → LOW confidence warning
```

**Progression Bonus** = Multi-message patterns (+0 to +100)

**Context Multiplier** = Based on your activity (0.5x to 2.0x)

**Final Score** = (Base Score + Progression Bonus) × Context Multiplier

### Conversation Stages

Messages become more suspicious as conversations progress:

| Stage | Description | Score Multiplier |
|-------|-------------|-----------------|
| INITIAL | Normal chat | 0.5x (reduced) |
| SETUP | Introducing scam | 1.0x (normal) |
| TRANSITION | Moving off-platform | 1.5x (increased) |
| EXPLOITATION | Asking for credentials | 2.5x (high alert) |
| PRESSURE | Creating urgency | 3.0x (critical) |

---

## ➕ Adding New Patterns

### Adding to Phishing Language

**When to use:** If it's a general manipulation tactic (not specific to one scam type)

1. Open `phishing-language.json`
2. Add a new tactic:

```json
"new_tactic_name": {
  "description": "What this detects",
  "baseScore": 15,
  "combinationBonus": 10,
  "patterns": [
    {
      "description": "Pattern category",
      "weight": 1.0,
      "phrases": [
        "phrase 1",
        "phrase 2"
      ]
    }
  ]
}
```

**Weight System:**
- `1.0` = Normal weight
- `1.5` = High priority (e.g., "i'm staff" is worse than "trust me")
- `0.7` = Lower priority (e.g., casual phrases)

### Adding to Scam Types

**When to use:** If it's specific to one scam type

1. Open the appropriate `scamtype-*.json` file
2. Add to `pattern_groups`:

```json
"new_pattern_group": {
  "description": "Specific action or request",
  "score": 30,
  "phrases": [
    "specific phrase 1",
    "specific phrase 2"
  ]
}
```

3. Add combination rule:

```json
{
  "description": "New pattern + existing",
  "requires": ["new_pattern_group", "existing_group"],
  "bonus": 55
}
```

### Creating a New Scam Type File

**NEW: Auto-Discovery!** Just create the file and reload - no code editing needed!

1. Copy an existing scam type file as a template
2. Rename: `scamtype-your-name.json` (MUST start with `scamtype-`)
3. Update the `id` and `name` fields:

```json
{
  "id": "your_scam_id",
  "name": "Your Scam Display Name",
  "description": "What this scam type detects",
  "base_score": 30,
  "pattern_groups": {
    "pattern_1": {
      "description": "What this pattern detects",
      "score": 25,
      "phrases": [
        "phrase 1",
        "phrase 2"
      ]
    }
  },
  "combination_rules": [
    {
      "description": "Pattern 1 + another = bonus",
      "requires": ["pattern_1", "another_pattern"],
      "bonus": 50
    }
  ]
}
```

4. Save file to `packcore/scamshield/` directory

5. Reload: `/scamshield reload`

6. Test: `/scamshield test <message>`

**That's it!** The system automatically discovers and loads all `scamtype-*.json` files.

**Important:**
- ✅ File name MUST start with `scamtype-`
- ✅ File name MUST end with `.json`
- ✅ Valid examples: `scamtype-crypto.json`, `scamtype-my-custom.json`
- ❌ Invalid: `custom-scam.json`, `scamtype.json`, `myscam.json`

---

## 🎮 Commands

### Main Commands

| Command | Description |
|---------|-------------|
| `/scamshield toggle` | Enable/disable ScamShield |
| `/scamshield reload` | Reload all patterns & discover new files |
| `/scamshield stats` | View detection statistics |
| `/scamshield clear` | Clear detection history |

**What `/scamshield reload` does:**
- 🔄 Reloads all existing `scamtype-*.json` files (picks up edits)
- 🔍 Scans for NEW `scamtype-*.json` files and loads them
- 🗑️ Removes deleted scam types from memory
- 💾 Clears the analysis cache for fresh analysis
- 📋 Shows summary of loaded detectors in logs

### Testing

| Command | Description |
|---------|-------------|
| `/scamshield test <message>` | Test a single message |
| `/scamshield debug` | Run full test suite (~30 seconds) |

**Example:**
```
/scamshield test quick! join my discord to verify your account

Output:
⚠ SCAM DETECTED
Category: Discord Verification
Score: 130
Patterns: urgency, discord_invitation, verification_request
```

### Whitelist Management

| Command | Description |
|---------|-------------|
| `/scamshield whitelist add <player>` | Add player to whitelist |
| `/scamshield whitelist remove <player>` | Remove from whitelist |
| `/scamshield whitelist list` | Show all whitelisted players |
| `/scamshield whitelist clear` | Clear entire whitelist |

---

## 🔧 Troubleshooting

### "Friend's message triggered warning"

**Solution:** Whitelist them
```
/scamshield whitelist add FriendName
```

### "Too many false positives"

**Solutions:**
1. Raise threshold: Edit config, set to 120-150
2. Remove overly aggressive phrases from JSON files
3. Add legitimate trade phrases to `LegitimateTradeContext.java`

### "Missed an obvious scam"

**Solutions:**
1. Test the message: `/scamshield test <message>`
2. Check which patterns matched (if any)
3. Add missing phrases to appropriate JSON file
4. `/scamshield reload`

### "Changes not taking effect"

Always run after editing files:
```
/scamshield reload
```

This command now:
- ✅ Reloads all existing scam type files
- ✅ Discovers and loads new scam type files
- ✅ Removes deleted scam type files
- ✅ Clears the analysis cache

### "New scam type file not loading"

Check the file name:
- ✅ Must start with `scamtype-`
- ✅ Must end with `.json`
- ✅ Must be in `packcore/scamshield/` directory

**Example valid names:**
- `scamtype-crypto.json`
- `scamtype-my-custom-scam.json`
- `scamtype-phishing-v2.json`

**Invalid names:**
- `crypto-scam.json` (doesn't start with scamtype-)
- `scamtype.json` (missing name part)
- `my-scam.json` (wrong prefix)

After creating the file, check logs:
```
[ScamShield] ✓ Loaded: Your Scam Name (scamtype-your-file.json)
```

Or for errors:
```
[ScamShield] ✗ Failed to load: scamtype-your-file.json
```

### "Pattern not matching"

**Common issues:**
- Message is normalized (lowercase, no special chars)
- Pattern must be substring match
- Check for typos in phrases array

**Debug:**
```
/scamshield test <exact message from chat>
```

### "Score too high/low"

**Adjust in JSON:**
- Pattern group `score` values
- Combination rule `bonus` values
- Or change detection threshold in config

---

## 📝 Best Practices

### DO:
✅ Test changes with `/scamshield test` before using  
✅ Keep backups of JSON files before major edits  
✅ Use descriptive names for new pattern groups  
✅ Add comments in `description` fields  
✅ Run `/scamshield reload` after every change

### DON'T:
❌ Set scores too high (causes false positives)  
❌ Duplicate patterns across files (causes inflated scores)  
❌ Add generic words like "the" or "you" as patterns  
❌ Edit `detections.json` manually (auto-generated)  
❌ Forget to test after adding new patterns

---

## 🆘 Getting Help

- **Discord:** [Your Support Server]
- **GitHub Issues:** [Your Repo]
- **In-game:** `/scamshield debug` for diagnostic info

---

## 📜 Example Use Cases

### Reducing False Positives for Lowballers

Lowballers legitimately use `/visit` commands. The system already handles this, but you can adjust:

Edit `LegitimateTradeContext.java` (requires Java knowledge) or lower visit command scores in `island-theft.json`.

### Adding a New Crypto Scam Pattern

If scammers start using cryptocurrency phrases:

1. Open `scamtype-discord-verify.json` (most likely category)
2. Add new pattern group:

```json
"crypto_mention": {
"description": "Cryptocurrency-related scams",
"score": 35,
"phrases": [
"bitcoin",
"btc",
"cryptocurrency",
"send crypto",
"wallet address"
]
}
```

3. Add combination:

```json
{
  "description": "Discord + Crypto = crypto scam",
  "requires": ["discord_invitation", "crypto_mention"],
  "bonus": 65
}
```

4. Save and `/scamshield reload`

---

## 🎯 Quick Reference

**File to edit depends on what you're adding:**

| What to Add | File to Edit |
|-------------|--------------|
| General manipulation tactic | `phishing-language.json` |
| Discord/verification phrase | `scamtype-discord-verify.json` |
| Island/co-op phrase | `scamtype-island-theft.json` |
| Trade/auction phrase | `scamtype-trade-manipulation.json` |
| Free reward phrase | `scamtype-free-rank.json` |

**After ANY edit:** `/scamshield reload`

**Testing:** `/scamshield test your message here`

**View results:** `/scamshield stats`

---

*Last updated: [Date] | Version: 4.0.0*