package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiAdvisor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePractice(
        session: PracticeSession,
        userQuery: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackAnalysis(session, userQuery)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

            val prompt = buildString {
                appendLine("You are an Iranian classical music master and Santoor (سنتور) expert.")
                appendLine("Analyze the following Santoor practice session and provide encouragement, technical Santoor advice, and Persian music (Radif) insights in Persian (فارسی):")
                appendLine("- عنوان قطعه/تمرین: ${session.title}")
                appendLine("- دستگاه/کوک: ${session.dastgah}")
                appendLine("- مدت تمرین: ${session.durationSeconds / 60} دقیقه و ${session.durationSeconds % 60} ثانیه")
                appendLine("- میانگین تمپو (BPM): ${session.avgBpm}")
                appendLine("- تعداد نت‌های نواخته شده: ${session.notesCount}")
                appendLine("- دقت ژوست و کوک نت‌ها: ${session.accuracyScore}%")
                if (session.userNotes.isNotBlank()) {
                    appendLine("- یادداشت هنرجو: ${session.userNotes}")
                }
                if (userQuery.isNotBlank()) {
                    appendLine("- سوال مشخص هنرجو: $userQuery")
                }
                appendLine("لطفاً پاسخی کاربردی، صمیمی، دقیق و به زبان فارسی همراه با راهکارهای تکنیکی مضراب‌نوازی و حفظ کوک ارائه بده.")
            }

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "high")
                    })
                }
                put("generationConfig", generationConfig)

                put("systemInstruction", JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "شما استاد برجسته و دلسوز ساز سنتور و ردیف موسیقی اصیل ایرانی هستید. تمام پاسخ‌ها را به زبان فارسی روان، تخصصی و در عین حال گرم و مشوقانه ارائه می‌دهید.")
                        })
                    }
                    put("parts", sysParts)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val resJson = JSONObject(responseStr)
                val candidates = resJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", fallbackAnalysis(session, userQuery))
                    }
                }
            }
            fallbackAnalysis(session, userQuery)
        } catch (e: Exception) {
            fallbackAnalysis(session, userQuery)
        }
    }

    private fun fallbackAnalysis(session: PracticeSession, userQuery: String): String {
        val durationMin = session.durationSeconds / 60
        val isAccurate = session.accuracyScore >= 80
        val isFast = session.avgBpm >= 110

        return buildString {
            append("تمرین بسیار ارزشمند «${session.title}» در ${session.dastgah}\n\n")
            append("تحلیل عملکرد تکنیکی مضراب و ریتم:\n")
            append("• مدت تمرین: $durationMin دقیقه و ${session.durationSeconds % 60} ثانیه با میانگین تمپوی ${session.avgBpm} ضرب در دقیقه.\n")

            if (isAccurate) {
                append("• ژوست بودن نت‌ها: فوق‌العاده است! شاخص دقت ${session.accuracyScore}٪ نشان‌دهنده کوک عالی سنتور و فرود دقیق مضراب روی سیم‌هاست.\n")
            } else {
                append("• ژوست بودن نت‌ها: دقت شما ${session.accuracyScore}٪ ثبت شد. پیشنهاد می‌شود روی ضربه زدن در فاصله مناسب از خرک (حدود ۴ تا ۵ سانتی‌متر) تمرکز کنید تا ارتعاش شفاف‌تر باشد.\n")
            }

            if (isFast) {
                append("• تمپو و وزن: تمپوی شما (${session.avgBpm} BPM) پرانرژی و در محدوده رنگ/چهارمضراب است. اطمینان حاصل کنید دست چپ و راست تعادل آکسان‌ها را در مضراب چپ حفظ کنند.\n")
            } else {
                append("• تمپو و وزن: سرعت آرام (${session.avgBpm} BPM) برای تثبیت گوشه‌ها و انگشت‌گذاری دقیق بسیار مناسب است. تمرین با مترونوم را در بازه‌های ۳ الی ۵ دقیقه‌ای تکرار کنید.\n")
            }

            append("\nنکته کلیدی استاد سنتور:\n")
            append("«همواره به صدادهی سیم‌های زرد و پشت خرک دقت کنید؛ سنتور مانند آینه روح است، لطافت در لمس مضراب، نغمه‌ای ماندگار می‌آفریند.»")
        }
    }
}
