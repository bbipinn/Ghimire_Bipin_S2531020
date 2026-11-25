package com.example.ghimire_bipin_s2531020;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private Button startButton;
    private ArrayList<TrafficSituation> allSituations;
    private SituationViewModel viewModel;
    private EditText roadSearch, distanceSearch;

    private ExecutorService executor;            // background thread pool for downloads
    private ScheduledExecutorService scheduler;  // triggers the automatic refresh
    private static final long REFRESH_MINUTES = 5;


    //DOWNLOADING FROM TRAFFIC SCOTLAND
    final String clientId = BuildConfig.clientId;
    final String clientKey = BuildConfig.clientKey;
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    final String currentRoadWorksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(SituationViewModel.class);


        roadSearch = findViewById(R.id.roadSearch);
        roadSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                viewModel.setRoadQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        distanceSearch = findViewById(R.id.distanceSearch);
        distanceSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                try {
                    viewModel.setMaxDistanceMiles(s.length() == 0 ? -1 : Double.parseDouble(s.toString()));
                } catch (NumberFormatException e) {
                    viewModel.setMaxDistanceMiles(-1);   // e.g. just "." typed
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Background thread pool for the two feed downloads
        executor = Executors.newFixedThreadPool(2);

        // Show when the data was last refreshed
        TextView lastUpdatedText = findViewById(R.id.lastUpdated);
        viewModel.getLastUpdated().observe(this, lastUpdatedText::setText);
    }

    public void onClick(View aview) {
        startProgress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            startProgress();
            Toast.makeText(this, "Refreshing…", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.action_map) {
            showMap();
            return true;
        }
        else if (id == R.id.action_list) {
            // Go back to the list, whatever screen were on
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return true;
        }
        else if (id == R.id.action_clear) {
            roadSearch.setText("");
            distanceSearch.setText("");
            return true;
        }
        else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("Traffic data: Traffic Scotland (DATEX II feed).\n\n"
                            + "Stripe colour = how long the works last:\n"
                            + "Green: under a day · Amber: under a week · Red: over a week\n\n"
                            + "Icons: Google Material Icons (Apache 2.0 licence).")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMap() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.listContainer, new SituationMapFragment())
                .addToBackStack(null)
                .commit();
    }

    public void startProgress() {
        // Both feeds download in parallel on the pool, not on new raw Threads
        executor.execute(new Task(unplannedEventsURL, "unplannedContentExample1.xml", false));
        executor.execute(new Task(currentRoadWorksURL, "roadworksContentExample.xml", true));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Load at once if there's no data yet after a rotation, wait for the next cycle
        long firstDelay = viewModel.hasData() ? REFRESH_MINUTES : 0;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::startProgress,
                firstDelay, REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void onStop() {
        super.onStop();
        scheduler.shutdownNow();   // no refreshing while the app is in the background
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private class Task implements Runnable {
        private String result = "";
        private String status;
        private final String myurl;
        private final String assetName;
        private final boolean isRoadworks;
        private final ArrayList<TrafficSituation> parsed = new ArrayList<>();

        public Task(String aurl, String assetName, boolean isRoadworks) {
            this.myurl = aurl;
            this.assetName = assetName;
            this.isRoadworks = isRoadworks;
        }

        @Override
        public void run() {
            Log.d("MyTask", "in run");

            // Download
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(myurl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(15000);
                urlConnection.setRequestProperty("Authorization",
                        Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                result = readAll(urlConnection.getInputStream());
            }
            catch (IOException e) {
                Log.e("urlConnection", "Download failed: " + e.getMessage(), e);
                result = "";
            }
            finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            // Decide which data to use
            if (result.contains("<?")) {
                saveCache(result);   // keep the most recent good download
                status = "Last updated: " + new SimpleDateFormat("HH:mm:ss", Locale.UK).format(new Date());
            }
            else {
                result = readCache();   // offline most recent saved download
                if (result != null) {
                    status = "Offline – showing saved data from " + cacheTime();
                }
                else {
                    result = readAsset();   // first run with no connection example file
                    if (result == null) {
                        viewModel.setStatus("Offline – no data available");
                        return;
                    }
                    status = "Offline – showing example data";
                }
            }

            int i = result.indexOf("<?");
            if (i >= 0) {
                result = result.substring(i);
            } else {
                Log.e("Download", "No XML found");
                viewModel.setStatus("Data could not be read");
                return;
            }

            // Parse
            try {

                boolean insideASituation = false;
                boolean insideGeneralPublicComment = false;
                boolean insideLocation = false;
                boolean insideFrom = false;
                boolean insideTo = false;
                boolean insideName = false;
                String pendingValue = null;
                String pendingType = null;

                TrafficSituation aSituation = null;

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(result));
                int eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {

                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("situationRecord")) {
                            insideASituation = true;
                            aSituation = new TrafficSituation();
                            aSituation.setType(xpp.getAttributeValue(0));
                            Log.d("MyTag", "New situation record found: " + xpp.getAttributeValue(0));

                        }
                        else if (!insideASituation) {
                            // ignore anything outside a record

                        }
                        else if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                            insideGeneralPublicComment = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("value") && insideGeneralPublicComment) {
                            aSituation.setDescription(xpp.nextText());
                        }
                        else if (xpp.getName().equalsIgnoreCase("overallStartTime")) {
                            aSituation.setStartTime(xpp.nextText());
                        }
                        else if (xpp.getName().equalsIgnoreCase("overallEndTime")) {
                            aSituation.setEndTime(xpp.nextText());
                        }
                        else if (xpp.getName().equalsIgnoreCase("delaysType")) {
                            aSituation.setDelayType(xpp.nextText());
                        }
                        else if (xpp.getName().equalsIgnoreCase("groupOfLocations")) {
                            insideLocation = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("from") && insideLocation) {
                            insideFrom = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("to") && insideLocation) {
                            insideTo = true;
                        }
                        else if (xpp.getName().equalsIgnoreCase("latitude") && insideLocation) {
                            try {
                                double v = Double.parseDouble(xpp.nextText().trim());
                                if (insideFrom) aSituation.setStartLat(v);
                                else if (insideTo) aSituation.setEndLat(v);
                            } catch (NumberFormatException e) {
                                Log.w("Parsing", "Bad latitude ignored");
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("longitude") && insideLocation) {
                            try {
                                double v = Double.parseDouble(xpp.nextText().trim());
                                if (insideFrom) aSituation.setStartLon(v);
                                else if (insideTo) aSituation.setEndLon(v);
                            } catch (NumberFormatException e) {
                                Log.w("Parsing", "Bad longitude ignored");
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("name") && insideLocation) {
                            insideName = true;
                            pendingValue = null;
                            pendingType = null;
                        }
                        else if (xpp.getName().equalsIgnoreCase("value") && insideName) {
                            pendingValue = xpp.nextText();
                        }
                        else if (xpp.getName().equalsIgnoreCase("tpegOtherPointDescriptorType") && insideName) {
                            pendingType = xpp.nextText();
                        }

                    }
                    else if (eventType == XmlPullParser.END_TAG) {
                        if (xpp.getName().equalsIgnoreCase("situationRecord")) {
                            String road = aSituation.getRoadName();
                            if (road == null || !road.matches("(?i)[AM]\\d+.*")) {
                                String fromText = roadFromText(aSituation.getDescription());
                                if (fromText != null) aSituation.setRoadName(fromText);
                            }
                            parsed.add(aSituation);
                            insideASituation = false;
                            insideLocation = false;
                        }
                        else if (xpp.getName().equalsIgnoreCase("generalPublicComment")) {
                            insideGeneralPublicComment = false;
                        }
                        else if (xpp.getName().equalsIgnoreCase("groupOfLocations")) {
                            insideLocation = false;
                        }
                        else if (xpp.getName().equalsIgnoreCase("from")) {
                            insideFrom = false;
                        }
                        else if (xpp.getName().equalsIgnoreCase("to")) {
                            insideTo = false;
                        }
                        else if (xpp.getName().equalsIgnoreCase("name") && insideName) {
                            if (pendingValue != null) {
                                if ("linkName".equalsIgnoreCase(pendingType)) {
                                    aSituation.setRoadName(pendingValue);
                                } else if ("nonLinkedPointName".equalsIgnoreCase(pendingType)
                                        && aSituation.getLocationText() == null) {
                                    aSituation.setLocationText(pendingValue);
                                }
                            }
                            insideName = false;
                        }
                    }

                    eventType = xpp.next();
                }

            } catch (XmlPullParserException e) {
                Log.e("Parsing", "EXCEPTION " + e);
            } catch (IOException e) {
                Log.e("Parsing", "IO error", e);
            }

            if (isRoadworks) viewModel.postRoadworks(parsed);
            else viewModel.postUnplanned(parsed);
            viewModel.setStatus(status);
        }

        private String roadFromText(String text) {
            if (text == null) return null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b[AM]\\d+\\b").matcher(text);
            return m.find() ? m.group() : null;
        }

        private String cacheName() {
            return isRoadworks ? "roadworks_cache.xml" : "unplanned_cache.xml";
        }

        // Save the latest download to the apps private storage
        private void saveCache(String xml) {
            try (FileOutputStream out = openFileOutput(cacheName(), MODE_PRIVATE)) {
                out.write(xml.getBytes(StandardCharsets.UTF_8));
            }
            catch (IOException e) {
                Log.e("Cache", "Could not save cache", e);
            }
        }

        private String readCache() {
            try (InputStream in = openFileInput(cacheName())) {
                return readAll(in);
            }
            catch (IOException e) {
                return null;   // no saved data yet
            }
        }

        private String cacheTime() {
            long t = new File(getFilesDir(), cacheName()).lastModified();
            return new SimpleDateFormat("dd/MM HH:mm", Locale.UK).format(new Date(t));
        }

        private String readAsset() {
            try (InputStream in = getAssets().open(assetName)) {
                return readAll(in);
            }
            catch (IOException e) {
                Log.e("XML File", "Could not read example file", e);
                return null;
            }
        }

        private String readAll(InputStream in) throws IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}