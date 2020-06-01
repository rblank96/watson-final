/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.developer_cloud.android.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.android.library.camera.GalleryHelper;
import com.ibm.watson.language_translator.v3.LanguageTranslator;
import com.ibm.watson.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.language_translator.v3.model.TranslationResult;
import com.ibm.watson.language_translator.v3.util.Language;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;
import com.ibm.watson.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.tone_analyzer.v3.model.ToneScore;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.transform.Source;

public class MainActivity extends AppCompatActivity {
  private final String TAG = "MainActivity";

  private EditText input;
  private ImageButton mic;
  private Button translate;
  private ImageButton play;
  private TextView translatedText;
  private TextView toneText;

  private SpeechToText speechService;
  private TextToSpeech textService;
  private LanguageTranslator translationService;
  private ToneAnalyzer toneService;
  private ToneAnalysis tone;
  private String selectedTargetLanguage = Language.SPANISH;
  private String selectedSourceLanguage = Language.ENGLISH;

  private StreamPlayer player = new StreamPlayer();

  private MicrophoneHelper microphoneHelper;

  private MicrophoneInputStream capture;
  private boolean listening = false;

  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    microphoneHelper = new MicrophoneHelper(this);

    speechService = initSpeechToTextService();
    textService = initTextToSpeechService();
    translationService = initLanguageTranslatorService();
    toneService = initToneService();

    final RadioGroup targetLanguage = findViewById(R.id.target_language);
    final RadioGroup sourceLanguage = findViewById(R.id.source_language);
    input = findViewById(R.id.input);
    mic = findViewById(R.id.mic);
    translate = findViewById(R.id.translate);
    play = findViewById(R.id.play);
    translatedText = findViewById(R.id.translated_text);
    toneText = findViewById((R.id.tone));


    targetLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
          case R.id.spanish:
            selectedTargetLanguage = Language.SPANISH;
            if (sourceLanguage.getCheckedRadioButtonId() == R.id.spanish_source){
              targetLanguage.clearCheck();
            }
            break;
          case R.id.french:
            selectedTargetLanguage = Language.FRENCH;
            if (sourceLanguage.getCheckedRadioButtonId() == R.id.french_source){
              targetLanguage.clearCheck();
            }
            break;
          case R.id.italian:
            selectedTargetLanguage = Language.ITALIAN;
            if (sourceLanguage.getCheckedRadioButtonId() == R.id.italian_source){
              targetLanguage.clearCheck();
            }
            break;
          case R.id.english:
            selectedTargetLanguage = Language.ENGLISH;
            if (sourceLanguage.getCheckedRadioButtonId() == R.id.english_source){
              targetLanguage.clearCheck();
            }
            break;
        }
      }
    });

    sourceLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
          case R.id.spanish_source:
            selectedSourceLanguage = Language.SPANISH;
            if (targetLanguage.getCheckedRadioButtonId() == R.id.spanish) {
              sourceLanguage.clearCheck();
            }
            break;
          case R.id.french_source:
            selectedSourceLanguage = Language.FRENCH;
            if (targetLanguage.getCheckedRadioButtonId() == R.id.french){
              sourceLanguage.clearCheck();
            }
            break;
          case R.id.italian_source:
            selectedSourceLanguage = Language.ITALIAN;
            if (targetLanguage.getCheckedRadioButtonId() == R.id.italian){
              sourceLanguage.clearCheck();
            }

            break;
          case R.id.english_source:
            selectedSourceLanguage = Language.ENGLISH;

            if (targetLanguage.getCheckedRadioButtonId() == R.id.english){
              sourceLanguage.clearCheck();
            }
            break;
        }
      }
    });

    input.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          translate.setEnabled(false);
        } else {
          translate.setEnabled(true);
        }
      }
    });

    mic.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!listening) {
          // Update the icon background
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mic.setBackgroundColor(Color.GREEN);
            }
          });
          capture = microphoneHelper.getInputStream(true);
          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture),
                        new MicrophoneRecognizeDelegate());
              } catch (Exception e) {
                showError(e);
              }
            }
          }).start();

          listening = true;
        } else {
          // Update the icon background
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mic.setBackgroundColor(Color.LTGRAY);
            }
          });
          microphoneHelper.closeInputStream();
          listening = false;
        }
      }
    });

    translate.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        new TranslationTask().execute(input.getText().toString());
        // Call the service and get the tone
//        ToneOptions toneOptions = new ToneOptions.Builder()
//                .text(input.getText().toString())
//                .build();
//
//        final ToneAnalysis tone = toneService.tone(toneOptions).execute().getResult();
//        runOnUiThread(new Runnable() {
//          @Override
//          public void run() {
//            translatedText.setText(tone.toString());
//          }
//        });
      }
    });

    translatedText.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          play.setEnabled(false);
        } else {
          play.setEnabled(true);
        }
      }
    });

    play.setEnabled(false);

    play.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new SynthesisTask().execute(translatedText.getText().toString());
      }
    });
  }


  private void showTranslation(final String translation) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        translatedText.setText(translation);
      }
    });

    ToneOptions toneOptions = new ToneOptions.Builder()
            .text(input.getText().toString())
            .build();

    final ToneAnalysis tone = toneService.tone(toneOptions).execute().getResult();

    List<ToneScore> scores = tone.getDocumentTone()
            .getTones();

    String detectedTones = "";
    for(ToneScore score:scores) {
      if(score.getScore() > 0.5f) {
        detectedTones += score.getToneName() + " ";
      }
    }

    final String tonemessage =
            "The following emotions were detected:\n\n"
                    + detectedTones.toUpperCase();

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        toneText.setText(tonemessage);
      }
    });
  }

  private void showError(final Exception e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        e.printStackTrace();
        // Update the icon background
        mic.setBackgroundColor(Color.LTGRAY);
      }
    });
  }

  private void showMicText(final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        input.setText(text);
      }
    });
  }

  private void enableMicButton() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mic.setEnabled(true);
      }
    });
  }

  private SpeechToText initSpeechToTextService() {
    Authenticator authenticator = new IamAuthenticator(getString(R.string.speech_text_apikey));
    SpeechToText service = new SpeechToText(authenticator);
    service.setServiceUrl(getString(R.string.speech_text_url));
    return service;
  }

  private TextToSpeech initTextToSpeechService() {
    Authenticator authenticator = new IamAuthenticator(getString(R.string.text_speech_apikey));
    TextToSpeech service = new TextToSpeech(authenticator);
    service.setServiceUrl(getString(R.string.text_speech_url));
    return service;
  }

  private ToneAnalyzer initToneService() {
    Authenticator authenticator = new IamAuthenticator(getString(R.string.tone_apikey));
    ToneAnalyzer service = new ToneAnalyzer("2020-05-30", authenticator);
    service.setServiceUrl(getString(R.string.tone_url));
    return service;
  }

  private LanguageTranslator initLanguageTranslatorService() {
    Authenticator authenticator
            = new IamAuthenticator(getString(R.string.language_translator_apikey));
    LanguageTranslator service = new LanguageTranslator("2018-05-01", authenticator);
    service.setServiceUrl(getString(R.string.language_translator_url));
    return service;
  }

  private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
    return new RecognizeOptions.Builder()
            .audio(captureStream)
            .contentType(ContentType.OPUS.toString())
            .model("en-US_BroadbandModel")
            .interimResults(true)
            .inactivityTimeout(2000)
            .build();
  }

  private abstract class EmptyTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    // assumes text is initially empty
    private boolean isEmpty = true;

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (s.length() == 0) {
        isEmpty = true;
        onEmpty(true);
      } else if (isEmpty) {
        isEmpty = false;
        onEmpty(false);
      }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    public abstract void onEmpty(boolean empty);
  }

  private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback implements RecognizeCallback {
    @Override
    public void onTranscription(SpeechRecognitionResults speechResults) {
      System.out.println(speechResults);
      if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
        String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
        showMicText(text);
      }
    }

    @Override
    public void onError(Exception e) {
      try {
        // This is critical to avoid hangs
        // (see https://github.com/watson-developer-cloud/android-sdk/issues/59)
        capture.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      showError(e);
      enableMicButton();
    }

    @Override
    public void onDisconnected() {
      enableMicButton();
    }
  }

  private class TranslationTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      TranslateOptions translateOptions = new TranslateOptions.Builder()
              .addText(params[0])
              .source(selectedSourceLanguage)
              .target(selectedTargetLanguage)
              .build();
      TranslationResult result
              = translationService.translate(translateOptions).execute().getResult();
      String firstTranslation = result.getTranslations().get(0).getTranslation();
      showTranslation(firstTranslation);
      return "Did translate";
    }
  }

  private class SynthesisTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... params) {

      String v = null;

      if (selectedTargetLanguage == Language.FRENCH){
        v = SynthesizeOptions.Voice.FR_FR_RENEEV3VOICE;
      }

      else if (selectedTargetLanguage == Language.SPANISH){
        v = SynthesizeOptions.Voice.ES_ES_ENRIQUEV3VOICE;
      }

      else if (selectedTargetLanguage == Language.ITALIAN){
        v = SynthesizeOptions.Voice.IT_IT_FRANCESCAV3VOICE;
      }
      else {
        v = SynthesizeOptions.Voice.EN_US_LISAVOICE;
      }

      SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
              .text(params[0])
              .voice(v)
              .accept(HttpMediaType.AUDIO_WAV)
              .build();
      player.playStream(textService.synthesize(synthesizeOptions).execute().getResult());
      return "Did synthesize";
    }
  }

  /**
   * On request permissions result.
   *
   * @param requestCode the request code
   * @param permissions the permissions
   * @param grantResults the grant results
   */
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    switch (requestCode) {
      case MicrophoneHelper.REQUEST_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

}
