package com.mooga.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.mooga.popularmovies.adapters.MovieGridAdapter;
import com.mooga.popularmovies.data.MovieContract;
import com.mooga.popularmovies.models.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private GridView mGridView;

    private MovieGridAdapter mMovieGridAdapter;

    private static final String SORT_SETTING_KEY = "sort_setting";
    private static final String POPULAR = "popular";
    private static final String RATED = "top_rated";
    private static final String FAVORITE = "favorite";
    private static final String MOVIES_KEY = "movies";

    private String mSortBy = POPULAR;

    private ArrayList<Movie> mMovies = null;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_IMAGE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_DATE
    };

    public static final int COL_ID = 0;
    public static final int COL_MOVIE_ID = 1;
    public static final int COL_TITLE = 2;
    public static final int COL_IMAGE = 3;
    public static final int COL_OVERVIEW = 4;
    public static final int COL_RATING = 5;
    public static final int COL_DATE = 6;

    public MainActivityFragment() {
    }

    public static final int MAX_PAGES = 100;
    private boolean mIsLoading = false;
    private TextView mLoading;
    private int mPagesLoaded = 0;
    private int rPagesLoaded = 0;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        void onItemSelected(Movie movie);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_main, menu);

        MenuItem action_sort_by_popularity = menu.findItem(R.id.action_sort_by_popularity);
        MenuItem action_sort_by_rating = menu.findItem(R.id.action_sort_by_rating);
        MenuItem action_sort_by_favorite = menu.findItem(R.id.action_sort_by_favorite);

        if (mSortBy.contentEquals(POPULAR)) {
            if (!action_sort_by_popularity.isChecked()) {
                action_sort_by_popularity.setChecked(true);
            }
        } else if (mSortBy.contentEquals(RATED)) {
            if (!action_sort_by_rating.isChecked()) {
                action_sort_by_rating.setChecked(true);
            }
        } else if (mSortBy.contentEquals(FAVORITE)) {
            if (!action_sort_by_popularity.isChecked()) {
                action_sort_by_favorite.setChecked(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_sort_by_popularity:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                mSortBy = POPULAR;
                mPagesLoaded=0;
                mMovieGridAdapter.clear();
                startPopularLoading();
                return true;
            case R.id.action_sort_by_rating:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                mSortBy = RATED;
                rPagesLoaded=0;
                mMovieGridAdapter.clear();
                startRatedLoading();
                return true;
            case R.id.action_sort_by_favorite:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                mSortBy = FAVORITE;
                mMovieGridAdapter.clear();
                startFavoriteLoading();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mGridView = (GridView) view.findViewById(R.id.gridview_movies);


        mMovieGridAdapter = new MovieGridAdapter(getActivity(), new ArrayList<Movie>());

        mGridView.setAdapter(mMovieGridAdapter);

        mLoading = (TextView) view.findViewById(R.id.loading);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Movie movie = mMovieGridAdapter.getItem(position);
                ((Callback) getActivity()).onItemSelected(movie);
            }
        });


        //on Scroll load movies
        mGridView.setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        int lastInScreen = firstVisibleItem + visibleItemCount;
                        if (lastInScreen == totalItemCount) {

                            if (mSortBy.contentEquals(POPULAR)) {
                                startPopularLoading();
                            } else if (mSortBy.contentEquals(RATED)) {
                                startRatedLoading();
                            }

                        }

                    }
                }
        );





        return view;
    }




    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!mSortBy.contentEquals(POPULAR)) {
            outState.putString(SORT_SETTING_KEY, mSortBy);
        }
        if (mMovies != null) {
            outState.putParcelableArrayList(MOVIES_KEY, mMovies);
        }
        super.onSaveInstanceState(outState);
    }
    // Invoke Popular Movies in Background
    public class FetchPopularTask extends AsyncTask<Integer, Void, Collection<Movie>> {

        private final String LOG_TAG = FetchPopularTask.class.getSimpleName();

        @Override
        protected Collection<Movie> doInBackground(Integer... params) {

            if (params.length == 0) {
                return null;
            }

            int page = params[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonStr = null;

            try {
                final String BASE_URL = "http://api.themoviedb.org/3/movie/";
                final String API_PARAM_PAGE = "page";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath( POPULAR)
                        .appendQueryParameter(API_PARAM_PAGE, String.valueOf(page))
                        .appendQueryParameter(API_KEY_PARAM, getString(R.string.api_key))
                        .build();

                URL url = new URL(builtUri.toString());

                Log.d(LOG_TAG, "Query: "+url.toString() );
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMoviesDataFromJson(jsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
        private Collection<Movie> getMoviesDataFromJson(String jsonStr) throws JSONException {
            JSONObject movieJson = new JSONObject(jsonStr);
            JSONArray movieArray = movieJson.getJSONArray("results");

            List<Movie> results = new ArrayList<>();

            for(int i = 0; i < movieArray.length(); i++) {
                JSONObject movie = movieArray.getJSONObject(i);
                Movie movieModel = new Movie(movie);
                results.add(movieModel);
            }

            return results;
        }

        @Override
        protected void onPostExecute(Collection<Movie> movies) {
            if (movies == null) {
                Toast.makeText(
                        getActivity(),
                        "Server Error",
                        Toast.LENGTH_SHORT
                ).show();

                stopLoading();
                return;
            }

            mPagesLoaded++;

            stopLoading();

            mMovieGridAdapter.addAll(movies);
        }
    }

    // Invoke Rated Movies in Background
    public class FetchRatedTask extends AsyncTask<Integer, Void, Collection<Movie>> {

        private final String LOG_TAG = FetchRatedTask.class.getSimpleName();

        @Override
        protected Collection<Movie> doInBackground(Integer... params) {

            if (params.length == 0) {
                return null;
            }

            int page = params[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonStr = null;

            try {
                final String BASE_URL = "http://api.themoviedb.org/3/movie/";
                final String API_PARAM_PAGE = "page";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath( RATED)
                        .appendQueryParameter(API_PARAM_PAGE, String.valueOf(page))
                        .appendQueryParameter(API_KEY_PARAM, getString(R.string.api_key))
                        .build();

                URL url = new URL(builtUri.toString());

                Log.d(LOG_TAG, "Query Rated : "+url.toString() );
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMoviesDataFromJson(jsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
        private Collection<Movie> getMoviesDataFromJson(String jsonStr) throws JSONException {
            JSONObject movieJson = new JSONObject(jsonStr);
            JSONArray movieArray = movieJson.getJSONArray("results");

            List<Movie> results = new ArrayList<>();

            for(int i = 0; i < movieArray.length(); i++) {
                JSONObject movie = movieArray.getJSONObject(i);
                Movie movieModel = new Movie(movie);
                results.add(movieModel);
            }

            return results;
        }

        @Override
        protected void onPostExecute(Collection<Movie> movies) {
            if (movies == null) {
                Toast.makeText(
                        getActivity(),
                        "Server Error",
                        Toast.LENGTH_SHORT
                ).show();

                stopLoading();
                return;
            }

            rPagesLoaded++;

            stopLoading();

            mMovieGridAdapter.addAll(movies);
        }
    }

    // Invoke Favorite Movies in Background
    public class FetchFavoriteTask extends AsyncTask<Void, Void, Collection<Movie>> {

        private Context mContext;

        public FetchFavoriteTask(Context context) {
            mContext = context;
        }


        @Override
        protected Collection<Movie> doInBackground(Void... params) {
            Cursor cursor = mContext.getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null
            );
            Collection<Movie> results = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Movie movie = new Movie(cursor);
                    results.add(movie);
                } while (cursor.moveToNext());
                cursor.close();
            }

            return results;
        }

        @Override
        protected void onPostExecute(Collection<Movie> movies) {
            if (movies != null) {
                if (mMovieGridAdapter != null) {
                    mMovieGridAdapter.addAll(movies);
                }
                mMovies = new ArrayList<>();
                return;
            }
            if (movies == null) {
                return;
            }
            mMovies.addAll(movies);
        }
    }

    // When on scroll invoke it to reload the next page from popular
    private void startPopularLoading() {
        if (mIsLoading) {
            return;
        }

        if (mPagesLoaded >= MAX_PAGES) {
            return;
        }

        mIsLoading = true;

        if (mLoading != null) {
            mLoading.setVisibility(View.VISIBLE);
        }

        new FetchPopularTask().execute(mPagesLoaded + 1);
    }

    // When on scroll invoke it to reload the next page from rated
    private void startRatedLoading() {
        if (mIsLoading) {
            return;
        }

        if (rPagesLoaded >= MAX_PAGES) {
            return;
        }

        mIsLoading = true;

        if (mLoading != null) {
            mLoading.setVisibility(View.VISIBLE);
        }

        new FetchRatedTask().execute(rPagesLoaded + 1);
    }

    //Invoke Favorite Movies
    private void startFavoriteLoading() {
        new FetchFavoriteTask(getActivity()).execute();
    }

    //stop loading View
    private void stopLoading() {
        if (!mIsLoading) {
            return;
        }

        mIsLoading = false;

        if (mLoading != null) {
            mLoading.setVisibility(View.GONE);
        }
    }


}
