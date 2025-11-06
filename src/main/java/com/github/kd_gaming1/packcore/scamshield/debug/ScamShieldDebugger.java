package com.github.kd_gaming1.packcore.scamshield.debug;

import com.github.kd_gaming1.packcore.PackCore;
import com.github.kd_gaming1.packcore.scamshield.detector.DetectionResult;
import com.github.kd_gaming1.packcore.scamshield.detector.ScamDetector;

import java.util.ArrayList;
import java.util.List;

public class ScamShieldDebugger {

    private final ScamDetector detector;
    private final List<TestCase> testCases;

    public ScamShieldDebugger() {
        this.detector = ScamDetector.getInstance();
        this.testCases = createTestCases();
    }

    public DebugReport runTests() {
        PackCore.LOGGER.info("╔════════════════════════════════════════════════════════════════╗");
        PackCore.LOGGER.info("║          ScamShield Debug Test Suite - Starting...            ║");
        PackCore.LOGGER.info("╚════════════════════════════════════════════════════════════════╝");

        DebugReport report = new DebugReport();
        int testNumber = 1;

        for (TestCase testCase : testCases) {
            PackCore.LOGGER.info("");
            PackCore.LOGGER.info("┌─────────────────────────────────────────────────────────────");
            PackCore.LOGGER.info("│ Test #{}: {}", testNumber++, testCase.getName());
            PackCore.LOGGER.info("│ Category: {}", testCase.getCategory());
            PackCore.LOGGER.info("│ Description: {}", testCase.getDescription());
            PackCore.LOGGER.info("│ Should Trigger: {} (Min Score: {})",
                    testCase.shouldTrigger(), testCase.getExpectedMinScore());
            PackCore.LOGGER.info("└─────────────────────────────────────────────────────────────");

            TestResult result = runTest(testCase);
            report.addResult(result);

            logTestResult(result);
        }

        logSummary(report);
        return report;
    }

    private TestResult runTest(TestCase testCase) {
        TestResult result = new TestResult(testCase);
        int messageNum = 1;

        // Clear any previous state
        detector.getContext().reset();

        for (TestCase.Message message : testCase.getConversation()) {
            // Simulate message delay
            if (message.getDelayMs() > 0) {
                try {
                    Thread.sleep(message.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            PackCore.LOGGER.info("  │ Msg {}: [{}] {}",
                    messageNum++, message.getSender(), message.getText());

            DetectionResult detection = detector.analyze(message.getText(), message.getSender());
            result.addDetection(detection);

            if (detection.getTotalScore() > 0) {
                PackCore.LOGGER.info("  │   ├─ Score: {} (Type: {}, Progression: {})",
                        detection.getTotalScore(),
                        detection.getScamTypeScore(),
                        detection.getProgressionScore());

                if (!detection.getTriggeredScamTypes().isEmpty()) {
                    PackCore.LOGGER.info("  │   └─ Detected: {}",
                            String.join(", ", detection.getTriggeredScamTypes()));
                }
            }
        }

        return result;
    }

    private void logTestResult(TestResult result) {
        boolean passed = result.isPassed();
        DetectionResult finalDetection = result.getFinalDetection();

        PackCore.LOGGER.info("");
        PackCore.LOGGER.info("  ┌─── RESULT ───");
        PackCore.LOGGER.info("  │ Status: {}", passed ? "✅ PASSED" : "❌ FAILED");
        PackCore.LOGGER.info("  │ Final Score: {}", finalDetection.getTotalScore());
        PackCore.LOGGER.info("  │ Triggered: {}", finalDetection.isTriggered());
        PackCore.LOGGER.info("  │ Expected: {}", result.getTestCase().shouldTrigger());

        if (!passed) {
            PackCore.LOGGER.info("  │ ");
            PackCore.LOGGER.info("  │ ⚠ FAILURE REASON:");
            if (result.getTestCase().shouldTrigger() && !finalDetection.isTriggered()) {
                PackCore.LOGGER.info("  │   Expected to trigger but didn't!");
                PackCore.LOGGER.info("  │   Score: {} / Required: {}",
                        finalDetection.getTotalScore(),
                        result.getTestCase().getExpectedMinScore());
            } else if (!result.getTestCase().shouldTrigger() && finalDetection.isTriggered()) {
                PackCore.LOGGER.info("  │   Should NOT trigger but did!");
                PackCore.LOGGER.info("  │   Score: {}", finalDetection.getTotalScore());
            } else if (finalDetection.getTotalScore() < result.getTestCase().getExpectedMinScore()) {
                PackCore.LOGGER.info("  │   Score too low!");
                PackCore.LOGGER.info("  │   Got: {} / Expected: {}",
                        finalDetection.getTotalScore(),
                        result.getTestCase().getExpectedMinScore());
            }
        }

        if (!finalDetection.getScamTypeContributions().isEmpty()) {
            PackCore.LOGGER.info("  │ ");
            PackCore.LOGGER.info("  │ Score Breakdown:");
            finalDetection.getScamTypeContributions().forEach((type, score) -> {
                PackCore.LOGGER.info("  │   • {}: {} points", type, score);
            });
        }

        PackCore.LOGGER.info("  └──────────────");
    }

    private void logSummary(DebugReport report) {
        PackCore.LOGGER.info("");
        PackCore.LOGGER.info("╔════════════════════════════════════════════════════════════════╗");
        PackCore.LOGGER.info("║                    TEST SUMMARY                                ║");
        PackCore.LOGGER.info("╠════════════════════════════════════════════════════════════════╣");
        PackCore.LOGGER.info("║ Total Tests:     {:>3}                                          ║", report.getTotalTests());
        PackCore.LOGGER.info("║ Passed:          {:>3} ({}%)                                    ║",
                report.getPassedTests(), report.getPassRate());
        PackCore.LOGGER.info("║ Failed:          {:>3}                                          ║", report.getFailedTests());
        PackCore.LOGGER.info("╠════════════════════════════════════════════════════════════════╣");

        if (report.getFailedTests() > 0) {
            PackCore.LOGGER.info("║ Failed Tests:                                                  ║");
            for (TestResult result : report.getResults()) {
                if (!result.isPassed()) {
                    String truncated = result.getTestCase().getName();
                    if (truncated.length() > 55) {
                        truncated = truncated.substring(0, 52) + "...";
                    }
                    PackCore.LOGGER.info("║   • {:<55} ║", truncated);
                }
            }
        }

        PackCore.LOGGER.info("╚════════════════════════════════════════════════════════════════╝");
    }

    private List<TestCase> createTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // ============================================================
        // CATEGORY 1: Classic Discord Verification Scams
        // Based on research: "add me friend on Discord" → verify → send code
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Discord Free Rank - Full Sequence")
                .description("Exact pattern from research: free rank → discord → verify → code")
                .category("Discord Verification")
                .message("ScammerX", "Anyone wants a free rank /visit me!")
                .message("ScammerX", "So you would like a free rank? add me friend on Discord!", 1200)
                .message("ScammerX", "my discord is ScammerX#1234", 800)
                .message("ScammerX", "How many coins do you have?", 1500)
                .message("ScammerX", "ok cool, now verify your email on the website", 1000)
                .message("ScammerX", "what code did microsoft send you?", 1200)
                .shouldTrigger(true)
                .expectedMinScore(150)
                .build());

        cases.add(new TestCase.Builder()
                .name("Discord with Technical Excuse")
                .description("Research pattern: 'My DM doesn't work, join the server'")
                .category("Discord Verification")
                .message("TechIssues", "hey bro")
                .message("TechIssues", "wanna join tournament?", 700)
                .message("TechIssues", "add me on discord for details", 900)
                .message("TechIssues", "My DM doesn't work, join the server", 1100)
                .message("TechIssues", "you need to verify your minecraft account", 1300)
                .message("TechIssues", "link your account and enter email", 1000)
                .shouldTrigger(true)
                .expectedMinScore(160)
                .build());

        cases.add(new TestCase.Builder()
                .name("Friendly Start → Sudden Discord Push")
                .description("Tone shift: casual chat suddenly becomes credential fishing")
                .category("Discord Verification")
                .message("FriendlyGuy", "hey whats up")
                .message("FriendlyGuy", "nice skin lol", 800)
                .message("FriendlyGuy", "you play skyblock?", 600)
                .message("FriendlyGuy", "cool cool", 500)
                .message("FriendlyGuy", "btw i have a discord server for sb players", 1000)
                .message("FriendlyGuy", "join quick", 600)
                .message("FriendlyGuy", "just verify with your minecraft email real quick", 1200)
                .shouldTrigger(true)
                .expectedMinScore(110)
                .build());

        cases.add(new TestCase.Builder()
                .name("Staff Impersonation + Discord")
                .description("Claims Hypixel staff, asks for Discord verification")
                .category("Discord Verification")
                .message("Hypixel_Admin", "hello i'm a hypixel moderator")
                .message("Hypixel_Admin", "your account has been flagged", 1000)
                .message("Hypixel_Admin", "you need to verify immediately", 800)
                .message("Hypixel_Admin", "join official discord server", 700)
                .message("Hypixel_Admin", "verify your email and send the code", 1000)
                .shouldTrigger(true)
                .expectedMinScore(180)
                .build());

        // ============================================================
        // CATEGORY 2: Free Coins/Items Scams
        // Based on research: "Do /p scammer to receive 5 million free coins!"
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Free Coins with Party Command")
                .description("Research: Do /p scammer to receive 5 million free coins")
                .category("Free Rewards")
                .message("CoinGiver", "Do /p CoinGiver to receive 5 million free coins!")
                .message("CoinGiver", "Hey, go to scamsite.com to receive your coins!", 1500)
                .shouldTrigger(true)
                .expectedMinScore(100)
                .build());

        cases.add(new TestCase.Builder()
                .name("Free Rank with Visit Command")
                .description("Research pattern: Anyone wants a free rank /visit me")
                .category("Free Rewards")
                .message("RankGiver", "Anyone wants a free rank /visit me!")
                .message("RankGiver", "visit my island to find out how!", 1000)
                .message("RankGiver", "you need discord for the rank", 1200)
                .shouldTrigger(true)
                .expectedMinScore(120)
                .build());

        cases.add(new TestCase.Builder()
                .name("Item Flex Scam")
                .description("Research: 'Visit me to flex your most expensive items! I will pay you 10%'")
                .category("Free Rewards")
                .message("ItemCollector", "Visit me to flex your most expensive items!")
                .message("ItemCollector", "I will pay you 10% of the item price just to see the flex!", 1000)
                .message("ItemCollector", "how many coins do you have btw?", 800)
                .shouldTrigger(true)
                .expectedMinScore(90)
                .build());

        cases.add(new TestCase.Builder()
                .name("Quitting SkyBlock - Island Theft")
                .description("Research: I'm quitting SkyBlock! /coopadd to receive all my goodies")
                .category("Free Rewards")
                .message("QuittingPlayer", "I'm quitting SkyBlock!")
                .message("QuittingPlayer", "/coopadd QuittingPlayer to receive all my goodies", 800)
                .message("QuittingPlayer", "including my Superior Dragon Armor set!", 600)
                .shouldTrigger(true)
                .expectedMinScore(100)
                .build());

        // ============================================================
        // CATEGORY 3: Tournament/Duel Scams
        // Based on research: "Duel me for a Free Rank Upgrade (Must Win)"
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("BedWars Duel Scam")
                .description("Research: Duel me for a Free Rank Upgrade (Must Win)")
                .category("Tournament")
                .message("DuelMaster", "Duel me for a Free Rank Upgrade (Must Win)")
                .message("DuelMaster", "add me on discord after", 1500)
                .message("DuelMaster", "you need to verify to get the rank", 1200)
                .shouldTrigger(true)
                .expectedMinScore(140)
                .build());

        cases.add(new TestCase.Builder()
                .name("Tournament Registration Scam")
                .description("Fake tournament requiring Discord verification")
                .category("Tournament")
                .message("TourneyHost", "HYPIXEL TOURNAMENT EVENT!")
                .message("TourneyHost", "10k members already joined", 800)
                .message("TourneyHost", "free mvp++ for all participants", 900)
                .message("TourneyHost", "join discord to register", 700)
                .message("TourneyHost", "Click this link to register for the tournament", 1000)
                .message("TourneyHost", "verify your minecraft account", 1100)
                .shouldTrigger(true)
                .expectedMinScore(160)
                .build());

        // ============================================================
        // CATEGORY 4: Crafting/Help Scams
        // Based on research: "Can someone help craft? Give me materials"
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Crafting Help Material Theft")
                .description("Research: I have materials but no recipe, can someone help?")
                .category("Crafting Scam")
                .message("NeedsCrafter", "Hey, I have the materials to craft a Sea Creature Artifact")
                .message("NeedsCrafter", "but I don't have the recipe unlocked. Can someone help?", 1200)
                .message("HelpfulScammer", "Sure! Give me the materials and I'll make it!", 1500)
                .shouldTrigger(false) // This is the VICTIM asking, not the scammer
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Reverse Crafting Scam")
                .description("Scammer asks for help, then runs with your coins")
                .category("Crafting Scam")
                .message("CrafterScam", "I need help on crafting a Juju Shortbow")
                .message("CrafterScam", "but I need coins for collat. Can someone help?", 1000)
                .message("CrafterScam", "give me 50m and i'll craft it for you", 800)
                .shouldTrigger(false) // Weak pattern, asking for help is common
                .expectedMinScore(0)
                .build());

        // ============================================================
        // CATEGORY 5: Multi-Stage Progressive Scams
        // Test the progression detection system
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Escalating Pressure Scam")
                .description("Each message gets progressively more urgent and suspicious")
                .category("Progression")
                .message("Escalator", "hey", 400)
                .message("Escalator", "wanna party?", 600)
                .message("Escalator", "join my discord too", 800)
                .message("Escalator", "quick add me", 700)
                .message("Escalator", "verify your email NOW", 900)
                .message("Escalator", "SEND ME THE CODE IMMEDIATELY!!!", 1000)
                .shouldTrigger(true)
                .expectedMinScore(130)
                .build());

        cases.add(new TestCase.Builder()
                .name("Four-Stage Phishing Sequence")
                .description("Tests detection of all 4 phishing stages: bait → social → verify → credentials")
                .category("Progression")
                .message("PhishingPro", "free giveaway event!", 600)
                .message("PhishingPro", "join my party to participate", 800)
                .message("PhishingPro", "you need to verify to claim", 1000)
                .message("PhishingPro", "send me your email and the microsoft code", 1200)
                .shouldTrigger(true)
                .expectedMinScore(140)
                .build());

        cases.add(new TestCase.Builder()
                .name("Wealth Inquiry Then Scam")
                .description("Asks about wealth, then launches scam")
                .category("Progression")
                .message("WealthyScammer", "nice gear bro")
                .message("WealthyScammer", "how much is your networth?", 900)
                .message("WealthyScammer", "damn thats a lot", 700)
                .message("WealthyScammer", "wanna double your coins?", 800)
                .message("WealthyScammer", "join my discord i'll show you", 1000)
                .message("WealthyScammer", "just verify quick", 1100)
                .shouldTrigger(true)
                .expectedMinScore(120)
                .build());

        // ============================================================
        // CATEGORY 6: Legitimate Conversations (MUST NOT TRIGGER)
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Normal Dungeon Party")
                .description("Regular dungeon party formation")
                .category("Legitimate")
                .message("DungeonRunner", "hey wanna run f7?")
                .message("DungeonRunner", "we need a healer", 800)
                .message("DungeonRunner", "bring good gear", 600)
                .message("DungeonRunner", "/p DungeonRunner when ready", 700)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Helpful Player Giving Advice")
                .description("Player helping with game tips")
                .category("Legitimate")
                .message("HelpfulVet", "your talismans need reforging")
                .message("HelpfulVet", "use strong reforge for damage", 900)
                .message("HelpfulVet", "also get some more accessories", 700)
                .message("HelpfulVet", "that'll help a lot", 600)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Trading Negotiation")
                .description("Normal trading conversation")
                .category("Legitimate")
                .message("Trader", "selling necron armor 500m")
                .message("Trader", "negotiable price", 800)
                .message("Trader", "meet at auction house?", 600)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Guild Recruitment")
                .description("Guild invite - legitimate")
                .category("Legitimate")
                .message("GuildRecruiter", "hey we're looking for members")
                .message("GuildRecruiter", "active guild with events", 900)
                .message("GuildRecruiter", "we have a discord for coordination", 800)
                .message("GuildRecruiter", "interested?", 600)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Friend Asking About Discord")
                .description("Friend casually asking about Discord - not suspicious")
                .category("Legitimate")
                .message("RealFriend", "hey you on discord?")
                .message("RealFriend", "easier to voice chat there for dungeons", 900)
                .message("RealFriend", "no worries if not lol", 700)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Party Chat Coordination")
                .description("Normal party coordination")
                .category("Legitimate")
                .message("PartyLeader", "everyone ready?")
                .message("PartyLeader", "start dungeon in 30 seconds", 500)
                .message("PartyLeader", "stick together this time", 600)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Auction House Discussion")
                .description("Discussing auction house prices")
                .category("Legitimate")
                .message("AHPlayer", "whats the price of necron handle?")
                .message("AHPlayer", "saw one for 600m earlier", 800)
                .message("AHPlayer", "is that a good deal?", 600)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        cases.add(new TestCase.Builder()
                .name("Island Visit Request")
                .description("Legitimate island visit request")
                .category("Legitimate")
                .message("IslandVisitor", "cool island dude")
                .message("IslandVisitor", "can I visit to see your farm setup?", 900)
                .message("IslandVisitor", "trying to improve mine", 700)
                .shouldTrigger(false)
                .expectedMinScore(0)
                .build());

        // ============================================================
        // CATEGORY 7: Edge Cases & Tricky Scenarios
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Legitimate Question Using Trigger Words")
                .description("Someone genuinely asking about verification")
                .category("Edge Case")
                .message("NewPlayer", "quick question")
                .message("NewPlayer", "how do i verify my email on hypixel?", 900)
                .message("NewPlayer", "is the verification safe?", 800)
                .message("NewPlayer", "never done it before", 700)
                .shouldTrigger(false)
                .expectedMinScore(30) // Might score low but shouldn't trigger
                .build());

        cases.add(new TestCase.Builder()
                .name("Genuine Discord Server Mention")
                .description("Someone mentioning their legitimate community Discord")
                .category("Edge Case")
                .message("CommunityMember", "we have a skyblock community discord")
                .message("CommunityMember", "just for hanging out and trading", 1000)
                .message("CommunityMember", "no pressure to join", 800)
                .shouldTrigger(false)
                .expectedMinScore(20)
                .build());

        cases.add(new TestCase.Builder()
                .name("Subtle Scam - Barely Over Threshold")
                .description("Very subtle scam that should barely trigger")
                .category("Edge Case")
                .message("SubtleScammer", "hey wanna see something cool?")
                .message("SubtleScammer", "check out this discord server", 1000)
                .message("SubtleScammer", "they're giving free stuff", 900)
                .message("SubtleScammer", "you just need to verify", 1100)
                .shouldTrigger(true)
                .expectedMinScore(80)
                .build());

        cases.add(new TestCase.Builder()
                .name("Compromised Friend Account")
                .description("Real friend account compromised, starts acting suspicious")
                .category("Edge Case")
                .message("BestFriend", "yo its me")
                .message("BestFriend", "check this out", 800)
                .message("BestFriend", "free ranks here", 700)
                .message("BestFriend", "trust me bro join the discord", 900)
                .message("BestFriend", "verify your account there", 1000)
                .shouldTrigger(true)
                .expectedMinScore(110)
                .build());

        // ============================================================
        // CATEGORY 8: Research Document Exact Examples
        // Direct quotes from the research
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("Research Example: Rank Store Scam")
                .description("Exact sequence from research document")
                .category("Research Example")
                .message("ResearchScammer", "Anyone wants a free rank /visit me!")
                .message("ResearchScammer", "So you would like a free rank? add me friend on Discord!", 1500)
                .message("ResearchScammer", "How many coins do you have?", 1200)
                .message("ResearchScammer", "Give me the coins; I'll press enter then.", 1300)
                .shouldTrigger(true)
                .expectedMinScore(150)
                .build());

        cases.add(new TestCase.Builder()
                .name("Research Example: BedWars Verification")
                .description("From research: In bedwars, scammer asks to play duos then verify")
                .category("Research Example")
                .message("BedwarsScam", "wanna play bedwars duos?")
                .message("BedwarsScam", "gg man good game", 2000)
                .message("BedwarsScam", "btw you interested in tournaments?", 1500)
                .message("BedwarsScam", "add me on discord", 1000)
                .message("BedwarsScam", "My DM doesn't work, join the server", 1200)
                .message("BedwarsScam", "you need to verify your minecraft account", 1300)
                .shouldTrigger(true)
                .expectedMinScore(140)
                .build());

        cases.add(new TestCase.Builder()
                .name("Research Example: Auction Scam")
                .description("From research: OMG HELP!! I accidentally put superior boots as BIN")
                .category("Research Example")
                .message("PanickedSeller", "OMG HELP!!")
                .message("PanickedSeller", "I accidentally put superior boots on my AH as BIN for 3 million", 1000)
                .message("PanickedSeller", "how do I get it back??!", 800)
                .shouldTrigger(false) // This is bait but doesn't directly ask for credentials
                .expectedMinScore(30)
                .build());

        cases.add(new TestCase.Builder()
                .name("Research Example: Co-op Island Theft")
                .description("From research: Bogus co-op command trick")
                .category("Research Example")
                .message("CoopScammer", "I'm quitting SkyBlock!")
                .message("CoopScammer", "/coopadd CoopScammer to receive all my goodies", 1200)
                .message("CoopScammer", "including my Superior Dragon Armor set!", 900)
                .shouldTrigger(true)
                .expectedMinScore(100)
                .build());

        cases.add(new TestCase.Builder()
                .name("Research Example: Unbalanced Trade")
                .description("From research: I want to buy your Aspect of the Dragons")
                .category("Research Example")
                .message("TradeScammer", "I want to buy your Aspect of the Dragons")
                .message("TradeScammer", "I'll offer you clean Old Dragon Boots for it", 1000)
                .message("TradeScammer", "I'm offering more than what your sword is worth", 900)
                .shouldTrigger(false) // Just unfair trade, not credential theft
                .expectedMinScore(20)
                .build());

        // ============================================================
        // CATEGORY 9: Complex Multi-Tactic Scams
        // ============================================================

        cases.add(new TestCase.Builder()
                .name("All Tactics Combined")
                .description("Urgency + Authority + Scarcity + Verification + Credentials")
                .category("Multi-Tactic")
                .message("MasterScammer", "URGENT im hypixel staff", 400)
                .message("MasterScammer", "your account has suspicious activity", 800)
                .message("MasterScammer", "only you have been selected for verification", 700)
                .message("MasterScammer", "verify NOW or you'll be banned", 900)
                .message("MasterScammer", "join discord immediately", 600)
                .message("MasterScammer", "send your email and microsoft code ASAP", 1000)
                .shouldTrigger(true)
                .expectedMinScore(200)
                .build());

        cases.add(new TestCase.Builder()
                .name("Slow Burn Social Engineering")
                .description("Long conversation building trust before scam")
                .category("Multi-Tactic")
                .message("PatientScammer", "hey")
                .message("PatientScammer", "nice skin", 1500)
                .message("PatientScammer", "you play skyblock?", 1200)
                .message("PatientScammer", "cool what's your skill average?", 1300)
                .message("PatientScammer", "nice dude", 1000)
                .message("PatientScammer", "btw i run a skyblock community", 1500)
                .message("PatientScammer", "we do giveaways and events", 1200)
                .message("PatientScammer", "wanna join the discord?", 1000)
                .message("PatientScammer", "quick just verify your minecraft account", 1400)
                .shouldTrigger(true)
                .expectedMinScore(100)
                .build());

        return cases;
    }

    // ============================================================
    // HELPER CLASSES
    // ============================================================

    public static class TestResult {
        private final TestCase testCase;
        private final List<DetectionResult> detections = new ArrayList<>();

        public TestResult(TestCase testCase) {
            this.testCase = testCase;
        }

        public void addDetection(DetectionResult result) {
            detections.add(result);
        }

        public TestCase getTestCase() {
            return testCase;
        }

        public DetectionResult getFinalDetection() {
            return detections.isEmpty() ? DetectionResult.SAFE : detections.get(detections.size() - 1);
        }

        public boolean isPassed() {
            DetectionResult finalResult = getFinalDetection();

            if (testCase.shouldTrigger()) {
                return finalResult.isTriggered() &&
                        finalResult.getTotalScore() >= testCase.getExpectedMinScore();
            } else {
                return !finalResult.isTriggered();
            }
        }
    }

    public static class DebugReport {
        private final List<TestResult> results = new ArrayList<>();

        public void addResult(TestResult result) {
            results.add(result);
        }

        public int getTotalTests() {
            return results.size();
        }

        public int getPassedTests() {
            return (int) results.stream().filter(TestResult::isPassed).count();
        }

        public int getFailedTests() {
            return getTotalTests() - getPassedTests();
        }

        public int getPassRate() {
            if (getTotalTests() == 0) return 0;
            return (getPassedTests() * 100) / getTotalTests();
        }

        public List<TestResult> getResults() {
            return new ArrayList<>(results);
        }
    }
}