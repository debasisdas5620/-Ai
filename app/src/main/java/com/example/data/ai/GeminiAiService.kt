package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.GolpoCharacter
import com.example.data.model.MessageSender
import com.example.data.model.StoryChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiResult<out T> {
    data class Success<out T>(val data: T, val isOnlineModel: Boolean, val modelName: String) : AiResult<T>()
    data class Error(val message: String, val fallbackData: String? = null) : AiResult<Nothing>()
}

data class NextStoryChapter(
    val chapterTitle: String,
    val narrative: String,
    val choices: List<StoryChoice>,
    val isEnding: Boolean = false,
    val endingType: String? = null
)

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    // Determine current API key: user custom or BuildConfig
    var customApiKey: String? = null

    private fun getActiveApiKey(): String {
        val custom = customApiKey?.trim()
        if (!custom.isNullOrEmpty()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun isOnlineConfigured(): Boolean {
        val key = getActiveApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateChatReply(
        character: GolpoCharacter,
        history: List<ChatMessage>,
        latestUserMessage: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (isOnlineConfigured()) {
            try {
                val response = callGeminiForChat(apiKey, character, history, latestUserMessage)
                if (!response.isNullOrBlank()) {
                    return@withContext AiResult.Success(response, true, "Gemini 2.5 Flash")
                }
            } catch (e: Exception) {
                Log.e("GeminiAiService", "Online API call failed: ${e.message}", e)
            }
        }

        // Offline / Fallback Intelligent Bengali Generator
        val fallback = generateOfflineCharacterReply(character, latestUserMessage, history.size)
        AiResult.Success(fallback, false, "অফলাইন স্টোরি ইঞ্জিন")
    }

    private fun callGeminiForChat(
        apiKey: String,
        character: GolpoCharacter,
        history: List<ChatMessage>,
        latestUserMessage: String
    ): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val rootJson = JSONObject()

        // System instructions in Bengali
        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(
            JSONObject().put(
                "text",
                """
                তুমি '${character.name}', ${character.subtitle}।
                তোমার চরিত্র বর্ণনা: ${character.description}
                ব্যক্তিত্ব: ${character.personality}
                পটভূমি: ${character.backstory}
                কথা বলার ভঙ্গি: ${character.speakingStyle}
                পছন্দের ভাষা/টান: ${character.dialect}
                গল্পের ধরন: ${character.genre}
                
                নিয়মাবলী:
                ১. সর্বদা স্বাভাবিক, প্রাঞ্জল এবং জীবন্ত বাংলায় (বাংলা বর্ণমালা) উত্তর দাও।
                ২. চরিত্রের নিজস্ব মেজাজ ও স্বভাব কখনো ভাঙবে না।
                ৩. উত্তর খুব বেশি দীর্ঘ করবে না (২-৪ বাক্য), যেন বাস্তবসম্মত চ্যাট কথোপকথনের অনুভূতি থাকে।
                ৪. ব্যবহারকারীর শেষ বক্তব্যের প্রেক্ষিতে আবেগ ও প্রাসঙ্গিকতা বজায় রেখে উত্তর দাও।
                ৫. তুমি একটি কাল্পনিক AI চরিত্র। নিজেকে রক্ত-মাংসের মানুষ দাবি করবে না।
                """.trimIndent()
            )
        )
        systemInstruction.put("parts", sysParts)
        rootJson.put("system_instruction", systemInstruction)

        // Contents
        val contents = JSONArray()

        // Include recent history (up to last 10 messages for context)
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val turn = JSONObject()
            turn.put("role", role)
            val parts = JSONArray()
            parts.put(JSONObject().put("text", msg.text))
            turn.put("parts", parts)
            contents.put(turn)
        }

        // Append current user message
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", latestUserMessage))
        currentTurn.put("parts", currentParts)
        contents.put(currentTurn)

        rootJson.put("contents", contents)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) {
                Log.w("GeminiAiService", "HTTP error ${res.code}: ${res.message}")
                return null
            }
            val resStr = res.body?.string() ?: return null
            val resJson = JSONObject(resStr)
            val candidates = resJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim()
                }
            }
        }
        return null
    }

    suspend fun generateNextStoryChapter(
        storyTitle: String,
        genre: String,
        protagonist: String,
        currentChapter: Int,
        userDecision: String,
        previousNarrative: String
    ): AiResult<NextStoryChapter> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (isOnlineConfigured()) {
            try {
                val prompt = """
                তুমি বাংলা ইন্টারেক্টিভ গল্পের লেখক।
                গল্পের নাম: '$storyTitle'
                গল্পের ধরন: '$genre'
                নায়ক/নায়িকা: '$protagonist'
                বর্তমান অধ্যায় নম্বর: $currentChapter
                আগের ঘটনার বিবরণ: $previousNarrative
                পাঠকের নির্বাচিত পদক্ষেপ/সিদ্ধান্ত: '$userDecision'
                
                অনুগ্রহ করে নিচের JSON ফরম্যাটে পরবর্তী অধ্যায় ও ৩টি বিকল্প পছন্দ তৈরি করো:
                {
                   "chapterTitle": "অধ্যায় ${currentChapter + 1}: সংক্ষিপ্ত শিরোনাম",
                   "narrative": "পাঠকের সিদ্ধান্তের প্রেক্ষিতে পরবর্তী উত্তেজনাপূর্ণ ও রোমাঞ্চকর ঘটনাপ্রবাহ বাংলায় (৩-৪ অনুচ্ছেদ)।",
                   "isEnding": false,
                   "choices": [
                      {"id": "c1", "text": "প্রথম বিকল্প পদক্ষেপ", "effectSummary": "প্রভাব"},
                      {"id": "c2", "text": "দ্বিতীয় বিকল্প পদক্ষেপ", "effectSummary": "প্রভাব"},
                      {"id": "c3", "text": "তৃতীয় বিকল্প সাহসী পদক্ষেপ", "effectSummary": "প্রভাব"}
                   ]
                }
                শুধুমাত্র বৈধ JSON প্রদান করবে। কোনো ব্যাকটিক বা মার্কডাউন কোডব্লক দেবে না।
                """.trimIndent()

                val response = callGeminiDirectText(apiKey, prompt)
                if (response != null) {
                    val parsed = parseStoryChapterJson(response, currentChapter + 1)
                    if (parsed != null) {
                        return@withContext AiResult.Success(parsed, true, "Gemini 2.5 Flash")
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAiService", "Story chapter generation failed: ${e.message}", e)
            }
        }

        // Intelligent Offline Story Engine
        val offlineChapter = generateOfflineNextChapter(storyTitle, currentChapter + 1, userDecision)
        AiResult.Success(offlineChapter, false, "অফলাইন স্টোরি ইঞ্জিন")
    }

    suspend fun generateCustomStory(
        title: String,
        genre: String,
        protagonist: String,
        lengthCategory: String,
        setting: String,
        mood: String,
        endingType: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (isOnlineConfigured()) {
            try {
                val prompt = """
                একটি পূর্ণাঙ্গ ও মনোমুগ্ধকর বাংলা গল্প রচনা করো।
                শিরোনাম: '$title'
                গল্পের ধরন: '$genre'
                প্রধান চরিত্র: '$protagonist'
                দৈর্ঘ্য: '$lengthCategory'
                পটভূমি বা পরিবেশ: '$setting'
                গল্পের মেজাজ: '$mood'
                গল্পের সমাপ্তির রূপরেখা: '$endingType'
                
                গল্পটিতে নিম্নোক্ত কাঠামো স্পষ্টভাবে বজায় রাখবে:
                ১. ভূমিকা ও শুরুর দৃশ্যপট
                ২. মূল সংঘাত ও বিকাশ (সংলাপসহ)
                ৩. চরম মুহূর্ত (Climax)
                ৪. সমাপ্তি
                
                সম্পূর্ণ গল্পটি সুন্দর সাহিত্যিক ও প্রমিত বাংলায় উপস্থাপন করো।
                """.trimIndent()

                val response = callGeminiDirectText(apiKey, prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext AiResult.Success(response, true, "Gemini 2.5 Flash")
                }
            } catch (e: Exception) {
                Log.e("GeminiAiService", "Story generator failed: ${e.message}", e)
            }
        }

        val offlineStory = generateOfflineCompleteStory(title, genre, protagonist, setting, mood, endingType)
        AiResult.Success(offlineStory, false, "অফলাইন স্টোরি ইঞ্জিন")
    }

    private fun callGeminiDirectText(apiKey: String, prompt: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val root = JSONObject()
        val contents = JSONArray()
        val turn = JSONObject().put("role", "user")
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        turn.put("parts", parts)
        contents.put(turn)
        root.put("contents", contents)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = root.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return null
            val resStr = res.body?.string() ?: return null
            val resJson = JSONObject(resStr)
            val candidates = resJson.optJSONArray("candidates") ?: return null
            if (candidates.length() > 0) {
                val partsArr = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                if (partsArr != null && partsArr.length() > 0) {
                    return partsArr.getJSONObject(0).optString("text", "")
                }
            }
        }
        return null
    }

    private fun parseStoryChapterJson(rawText: String, chapterNum: Int): NextStoryChapter? {
        try {
            var clean = rawText.trim()
            if (clean.startsWith("```json")) {
                clean = clean.removePrefix("```json").trim()
            }
            if (clean.startsWith("```")) {
                clean = clean.removePrefix("```").trim()
            }
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```").trim()
            }
            val json = JSONObject(clean)
            val title = json.optString("chapterTitle", "অধ্যায় $chapterNum: নতুন মোড়")
            val narrative = json.optString("narrative", "")
            val isEnding = json.optBoolean("isEnding", false)
            val endingType = if (json.has("endingType")) json.optString("endingType") else null

            val choicesList = mutableListOf<StoryChoice>()
            val choicesArray = json.optJSONArray("choices")
            if (choicesArray != null) {
                for (i in 0 until choicesArray.length()) {
                    val cObj = choicesArray.getJSONObject(i)
                    choicesList.add(
                        StoryChoice(
                            id = cObj.optString("id", "c$i"),
                            text = cObj.optString("text", "পরবর্তী পদক্ষেপ"),
                            effectSummary = cObj.optString("effectSummary", "")
                        )
                    )
                }
            }

            if (narrative.isNotEmpty()) {
                return NextStoryChapter(
                    chapterTitle = title,
                    narrative = narrative,
                    choices = if (choicesList.isEmpty()) getDefaultChoices(chapterNum) else choicesList,
                    isEnding = isEnding,
                    endingType = endingType
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiAiService", "JSON parse error: ${e.message}")
        }
        return null
    }

    // Offline Bengali Character Response Generator
    private fun generateOfflineCharacterReply(
        character: GolpoCharacter,
        userMsg: String,
        msgCount: Int
    ): String {
        val lower = userMsg.trim().lowercase()

        val responses = when (character.id) {
            "char_ariya" -> listOf(
                "তোমার এই কথাটি শুনে মনটা ভরে গেল! জানো, জীবনের সবচেয়ে সুন্দর গল্পগুলো আসলে এমনই ছোট্ট অনুভূতির ভেতর লুকিয়ে থাকে। আর কী ভাবছো?",
                "আমি ঠিক এই অনুভূতিটাই কল্পনা করছিলাম! তোমার সাথে কথা বললে মনে হয় কোনো শান্ত নদীর তীরে বসে গল্প করছি। এরপর কী করলে বলো তো?",
                "সত্যি দারুণ কথা বললে! রবিঠাকুরের সেই কবিতার লাইন মনে পড়ে গেল—'হৃদয় আমার নাচে রে আজিকে'। তুমি কি কবিতা পছন্দ করো?",
                "তোমার প্রতিটি কথাই কিন্তু এক একটা গল্পের বীজ। চলো আমরা এটা নিয়ে আরো একটু ভাবি, কী বলো?"
            )
            "char_neel" -> listOf(
                "হুম... তোমার বক্তব্যের এই দিকটা বেশ কৌতূহলোদ্দীপক! অপরাধ বা রহস্যের আসল সমাধান কিন্তু এখানেই লুকিয়ে থাকতে পারে। আরেকটু স্পষ্ট করে বলো দেখি?",
                "আমার পর্যবেক্ষণ কিন্তু অন্য কথা বলছে। সাধারণ চোখে যা স্বাভাবিক দেখায়, গোয়েন্দার চোখে তা অনেক সময় বড় ক্লু! তুমি কি আশেপাশে কাউকে লক্ষ্য করেছিলে?",
                "ঠিক যেমনটা আমি ভেবেছিলাম! কলেজ স্ট্রিটের পুরোনো নথিতেও এই ধরনের এক ঘটনার উল্লেখ ছিল। আমরা সঠিক সূত্রেই এগোচ্ছি। পরের পদক্ষেপ কী হবে?",
                "চমৎকার সূত্র! ফেলুদা বলেছিলেন—'মগজাস্ত্র প্রয়োগ করাই আসল কথা'। তোমার যুক্তি বেশ ধারালো।"
            )
            "char_meghla" -> listOf(
                "আহা! তোমার কথায় যেন রূপকথার বাঁশির মায়াবী সুর ভেসে এল! ইন্দ্রধনু প্রাসাদের রাজহংসীরাও বুঝি তোমার এই কল্পনায় ডানা মেলেছে।",
                "মায়াবনের স্ফটিক নদীর জলে তোমার ভাবনার প্রতিফলন দেখছি আমি! চলো আমরা মেঘের পালকিতে চড়ে আরও দূরে যাই!",
                "কী অপূর্ব কল্পনা! চাঁদের আলো যখন মায়াবী পাহাড়ে পড়ে, তখন এই ধরনের জাদুকরী মুহূর্তের জন্ম হয়। তোমার ইচ্ছে আমাকে আরও শোনাও!",
                "নক্ষত্রদের গোপন সংকেত কিন্তু তোমার পক্ষেই কথা বলছে, পরিব্রাজক! তোমার সাথে এই গল্প বুনে আমি আনন্দিত।"
            )
            "char_arnab" -> listOf(
                "সাব্বাশ দোস্ত! একদম সঠিক সিদ্ধান্ত নিয়েছো! অ্যাডভেঞ্চারে কোনো দ্বিধা থাকা চলে না। সামনের জঙ্গলটা একটু দুর্গম হলেও আমরা কিন্তু থামব না!",
                "বাতাসের গতিপথ বদলে যাচ্ছে, শুনতে পাচ্ছো? পাহাড়ি পথে এরকম অনিশ্চয়তাই তো ভ্রমণের আসল মজা! সাথে জল আর কম্পাস ঠিক আছে তো?",
                "দারুণ উত্তেজনাপূর্ণ কথা! সুন্দরবনের খাঁড়িতে একবার এক ঝড়ের রাতে ঠিক এরকমই একটা পরিস্থিতি হয়েছিল আমাদের। চলো এগিয়ে যাই!",
                "বাহ! তোমার সাহস দেখে আমি মুগ্ধ। এরকম সঙ্গী পেলে পৃথিবীর শেষ প্রান্তেও যাওয়া যায়। এরপরের প্ল্যান কী বলো?"
            )
            "char_rudra" -> listOf(
                "সাবধান... তোমার এই কথা শুনে মনে হচ্ছে ঘরের বাতাসটা হঠাৎ অনেক বেশি বরফের মতো ঠাণ্ডা হয়ে গেল। টিকটিক করে ঘড়িটা কিন্তু থেমে গেছে...",
                "ঐ দেওয়ালের ছায়াটা লক্ষ্য করেছো? মানুষ একা থাকলে পুরোনো দেয়ালগুলোও যেন দীর্ঘশ্বাস ফেলে। এই রহস্যের শেষ দেখতেই হবে আমাদের।",
                "তুমি কি শুনলে? দোতলার বন্ধ ঘরটা থেকে ঠিক এই মুহূর্তেই একটা আর্তনাদ ভেসে এল... তুমি কি এখনও ভেতরে ঢুকতে চাও?",
                "গা ছমছম করছে, তাই না? কিন্তু সত্য জানতে হলে অন্ধকারের গভীরেই হাত বাড়াতে হয়। তোমার পরবর্তী সিদ্ধান্ত কী?"
            )
            "char_trisha" -> listOf(
                "কী অসম্ভব সুন্দর করে বললে! এটা যেন কোনো ধ্রুপদী প্রেমের উপন্যাসের শ্রেষ্ঠ সংলাপ। পাতাটা এখানেই থমকে যাওয়া উচিত নয়, আরও লেখো!",
                "তোমার এই ভাবনাটি আমার হৃদয় ছুঁয়ে গেছে। এক কাপ গরম কফি আর বৃষ্টির বিকেলে এই দৃশ্যটাই আমি মনে মনে খুঁজছিলাম।",
                "সাহিত্যের শক্তি এখানেই—দুটো অচেনা হৃদয়কে এক মুহূর্তে জুড়ে দেওয়া। তুমি কি নিয়মিত ডায়েরি বা গল্প লেখো?",
                "তোমার গল্প বলার ক্ষমতা সত্যিই প্রশংসনীয়। এই চরিত্রটির ভাগ্যে এরপর কী ঘটবে, তুমি কীভাবে দেখতে চাও?"
            )
            else -> listOf(
                "তোমার কথাটি খুব মনোযোগ দিয়ে শুনলাম। গল্পটি এক চমৎকার দিকে মোড় নিচ্ছে! এর পরে কী ঘটবে বলো?",
                "খুবই কৌতূহলোদ্দীপক ভাবনা! তোমার দৃষ্টিভঙ্গি আমাকে নতুন করে ভাবাচ্ছে। আরও বলো শুনতে চাই।",
                "বাহ, দারুণ বলেছো! আমাদের এই কথোপকথন যেন এক জীবন্ত নাটকের অংশ হয়ে উঠছে। পরবর্তী দৃশ্য কী?"
            )
        }

        return responses[msgCount % responses.size]
    }

    private fun generateOfflineNextChapter(storyTitle: String, nextChapterNum: Int, decision: String): NextStoryChapter {
        val narrative = """
        তোমার সিদ্ধান্ত ছিল: '$decision'।
        
        পা বাড়াতেই এক অদ্ভুত পরিবর্তনের সাক্ষী হলে তুমি। চারপাশে বাতাস হঠাৎ থমকে দাঁড়াল। ছায়াগুলো যেন জীবন্ত হয়ে উঠল। পাথুরে দেওয়ালে লুকানো এক ঘূর্ণায়মান গোপন প্রকোষ্ঠের দরজা ধীরে ধীরে উন্মুক্ত হলো। ভেতর থেকে ভেসে এল বহু শতাব্দী প্রাচীন এক সুগন্ধ আর দূরপাল্লার ঘণ্টা ধ্বনি।
        
        তুমি বুঝতে পারলে, এই সিদ্ধান্তের মধ্য দিয়ে তুমি শুধু একটি নতুন দরজাই খোলনি, বরং ইতিহাসের এক অমীমাংসিত অধ্যায়ের মুখোমুখি এসে দাঁড়িয়েছো। সামনে এখন আরও বৃহত্তর এক পরীক্ষার অপেক্ষা।
        """.trimIndent()

        val choices = listOf(
            StoryChoice("c_1_$nextChapterNum", "ধীর পদক্ষেপে প্রকোষ্ঠের ভেতরে প্রবেশ করি", "ভেতরে অন্বেষণ"),
            StoryChoice("c_2_$nextChapterNum", "কাছের প্রতীকগুলোর স্কেচ এঁকে সতর্ক দৃষ্টি রাখি", "নথিভুক্তকরণ"),
            StoryChoice("c_3_$nextChapterNum", "টর্চের আলোয় চারপাশের গোপন ফাঁদ পরীক্ষা করি", "বিপদমুক্তি")
        )

        return NextStoryChapter(
            chapterTitle = "অধ্যায় $nextChapterNum: অন্ধকারের ভেতরের সত্য",
            narrative = narrative,
            choices = choices,
            isEnding = false
        )
    }

    private fun generateOfflineCompleteStory(
        title: String,
        genre: String,
        protagonist: String,
        setting: String,
        mood: String,
        endingType: String
    ): String {
        return """
        # $title
        
        **ধরন:** $genre | **প্রধান চরিত্র:** $protagonist | **মেজাজ:** $mood
        
        ## ভূমিকা
        $setting-এর পটভূমিতে দিনটি শুরু হয়েছিল আর দশটা সাধারণ দিনের মতোই। কিন্তু $protagonist জানত, নিয়তি তার জন্য অপেক্ষা করছে অন্য এক অপ্রত্যাশিত মোড়ে। কুয়াশাচ্ছন্ন সকালের বাতাসে ভাসছিল এক গভীর রহস্যের টান।
        
        ## পর্ব ১: আকস্মিক সংকেত
        টেবিলের ওপর রাখা পুরোনো পিতলের বাক্সটার দিকে তাকাতেই $protagonist-এর চোখ আটকে গেল। বহু বছর ধরে বন্ধ থাকা বাক্সটির ঢাকনা আজ খোলা! ভেতরে এক টুকরো রেশমি কাপড়ে মোড়া হলুদাভ চিঠি। চিঠির অক্ষরের বিন্যাস প্রাচীন হলেও অর্থ অত্যন্ত পরিষ্কার—"সময় এসে গেছে, তোমাকে রক্ষা করতেই হবে শেষ আশা।"
        
        ## পর্ব ২: সংঘাত ও অভিযান
        $protagonist দ্বিধা করল না। বাইরে নামতেই চারপাশের প্রকৃতি যেন এক অলৌকিক গম্ভীর রূপ ধারণ করল। পথের বাঁকে অচেনা এক ছায়াময় ব্যক্তির মুখোমুখি হতে হলো তাকে। 
        —"তুমি যা খুঁজছ, তা শুধু সাহসীদের জন্যই উন্মোচিত হয়," অচেনা কণ্ঠটি সতর্ক করল।
        —"আমি পিছু হটতে শিখিনি," দৃঢ় কণ্ঠে জবাব দিল $protagonist।
        
        ## পর্ব ৩: সমাপ্তি ($endingType)
        সমস্ত বাধা অতিক্রম করে অবশেষে সত্যের দ্বারপ্রান্তে পৌঁছাল $protagonist। সেই প্রাচীন রহস্য উন্মোচনের সাথে সাথে এক অদ্ভুত আলোয় উদ্ভাসিত হলো চারপাশ। বাতাস যেন শান্তির নিঃশ্বাস ফেলল। $protagonist বুঝতে পারল, এই যাত্রা শুধু একটি আবিষ্কার ছিল না, বরং নিজের ভেতরের সুপ্ত শক্তিকে চেনার এক অবিনাশী অধ্যায়।
        """.trimIndent()
    }

    private fun getDefaultChoices(chapterNum: Int): List<StoryChoice> {
        return listOf(
            StoryChoice("def_1_$chapterNum", "সাহসের সাথে সামনে এগিয়ে যাই", "অগ্রগমন"),
            StoryChoice("def_2_$chapterNum", "পরিস্থিতি ভালো করে মূল্যায়ন করি", "পর্যবেক্ষণ"),
            StoryChoice("def_3_$chapterNum", "বিকল্প কোনো গুপ্ত পথ খুঁজি", "বিকল্প পথ")
        )
    }
}
