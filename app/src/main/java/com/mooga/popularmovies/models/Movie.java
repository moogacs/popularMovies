package com.mooga.popularmovies.models;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.mooga.popularmovies.MainActivityFragment;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by macbookpro on 1/10/15.
 */
public class Movie implements Parcelable {

    private int id;
    private String title;
    private String image;
    private String overview;
    private Double rating;
    private String date;

    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_OVERVIEW = "overview";
    public static final String KEY_POSTER_PATH = "image";
    public static final String KEY_VOTE_AVERAGE = "rating";
    public static final String KEY_RELEASE_DATE = "date";




    public Movie() {

    }

    
    public Movie(JSONObject movie) throws JSONException {
        this.id = movie.getInt("id");
        this.title = movie.getString("original_title");
        this.image = movie.getString("poster_path");
        this.overview = movie.getString("overview");
        this.rating = movie.getDouble("vote_average");
        this.date = movie.getString("release_date");
    }

    public Movie(Cursor cursor) {
        this.id = cursor.getInt(MainActivityFragment.COL_MOVIE_ID);
        this.title = cursor.getString(MainActivityFragment.COL_TITLE);
        this.image = cursor.getString(MainActivityFragment.COL_IMAGE);
        this.overview = cursor.getString(MainActivityFragment.COL_OVERVIEW);
        this.rating = cursor.getDouble(MainActivityFragment.COL_RATING);
        this.date = cursor.getString(MainActivityFragment.COL_DATE);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getOverview() {
        return overview;
    }

    public double getRating() {
        return rating;
    }

    public String getDate() {
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(image);
        dest.writeString(overview);
        dest.writeDouble(rating);
        dest.writeString(date);
    }

    public static final Parcelable.Creator<Movie> CREATOR
            = new Parcelable.Creator<Movie>() {
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    private Movie(Parcel in) {
        id = in.readInt();
        title = in.readString();
        image = in.readString();
        overview = in.readString();
        rating = in.readDouble();
        date = in.readString();
    }

}
