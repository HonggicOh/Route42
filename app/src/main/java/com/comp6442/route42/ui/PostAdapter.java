package com.comp6442.route42.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.comp6442.route42.R;
import com.comp6442.route42.data.FirebaseAuthLiveData;
import com.comp6442.route42.data.model.Post;
import com.comp6442.route42.data.repository.FirebaseStorageRepository;
import com.comp6442.route42.data.repository.PostRepository;
import com.comp6442.route42.ui.fragment.MapsFragment;
import com.comp6442.route42.ui.fragment.ProfileFragment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageReference;

import timber.log.Timber;

/* Class to feed Cloud Firestore documents into the FirestoreRecyclerAdapter */
public class PostAdapter extends FirestoreRecyclerAdapter<Post, PostAdapter.PostViewHolder> {
  private final String loggedInUID;

  public PostAdapter(@NonNull FirestoreRecyclerOptions<Post> options, String loggedInUID) {
    super(options);
    this.loggedInUID = loggedInUID;
  }

  @NonNull
  @Override
  public PostViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
    View view = LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.post_card, viewGroup, false);
    Timber.d("PostAdapter created.");
    return new PostViewHolder(view);
  }

  @Override
  protected void onBindViewHolder(@NonNull PostViewHolder viewHolder, int position, @NonNull Post post) {
    setViewBehavior(post, viewHolder);

    // set profile pic
    Timber.i("Fetched post: %s", post);

    if (post.getProfilePicUrl().startsWith("http")) {
      Glide.with(viewHolder.imageView.getContext())
              .load(post.getProfilePicUrl())
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .skipMemoryCache(false)
              .circleCrop()
              .into(viewHolder.userIcon);
    } else {
      // Get reference to the image file in Cloud Storage, download route image, use stock photo if fail
      StorageReference profilePicRef = FirebaseStorageRepository.getInstance().get(post.getProfilePicUrl());

      Glide.with(viewHolder.userIcon.getContext())
              .load(profilePicRef)
              .placeholder(R.drawable.unknown_user)
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .skipMemoryCache(false)
              .circleCrop()
              .into(viewHolder.userIcon);
    }

    Glide.with(viewHolder.imageView.getContext())
            .load(post.getImageUrl())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .centerCrop()
            .into(viewHolder.imageView);

    Timber.d("OnBindView complete.");
  }

  @Override
  public void onDataChanged() {
    //Called each time there is a new query snapshot.
    Timber.d("breadcrumb");
  }

  @Override
  public void onError(@NonNull FirebaseFirestoreException e) {
    //Handle the error
    Timber.d(e);
  }

  private void setViewBehavior(Post post, PostViewHolder viewHolder) {
    Timber.d("breadcrumb");
    // Add listener and navigate to the user's profile on click
    setUserNameView(post, viewHolder);
    setLikeCountTextView(post, viewHolder);

    viewHolder.userNameView.setText(post.getUserName());
    viewHolder.descriptionView.setText(post.getPostDescription());

    if (post.getHashtags().size() > 0)
      viewHolder.hashtagsTextView.setText(String.join(" ", post.getHashtags()));

    if (post.getLocationName() != null) {
      viewHolder.locationTextView.setText(post.getLocationName());
      viewHolder.locationTextView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Fragment fragment = new MapsFragment();
          Bundle bundle = new Bundle();
          bundle.putDouble("lat", post.getLatitude());
          bundle.putDouble("lon", post.getLongitude());
          fragment.setArguments(bundle);
          ((FragmentActivity) viewHolder.itemView.getContext()).getSupportFragmentManager()
                  .beginTransaction()
                  .add(R.id.fragment_container_view, fragment)
                  .addToBackStack(this.getClass().getCanonicalName())
                  .commit();
        }
      });
    } else {
      viewHolder.locationTextView.setText(" ");
      viewHolder.locationTextView.setText("");
    }

    Timber.d("OnBindView complete.");
  }

  private void setLikeButtons(Post post, PostViewHolder viewHolder, boolean postIsLiked) {
    viewHolder.like.setOnClickListener(view -> {
      PostRepository.getInstance().like(post, loggedInUID);
      viewHolder.like.setVisibility(View.GONE);
      viewHolder.unlike.setVisibility(View.VISIBLE);
      Timber.i("Liked");
    });

    viewHolder.unlike.setOnClickListener(view -> {
      PostRepository.getInstance().unlike(post, loggedInUID);
      viewHolder.unlike.setVisibility(View.GONE);
      viewHolder.like.setVisibility(View.VISIBLE);
      Timber.i("UnLiked");
    });

    if (postIsLiked) {
      viewHolder.like.setVisibility(View.GONE);
      viewHolder.unlike.setVisibility(View.VISIBLE);
    } else {
      viewHolder.unlike.setVisibility(View.GONE);
      viewHolder.like.setVisibility(View.VISIBLE);
    }
  }

  private void setLikeCountTextView(Post post, PostViewHolder viewHolder) {
    viewHolder.likeCountTextView.setText(String.valueOf(post.getLikeCount()));
    setLikeButtons(
            post,
            viewHolder,
            post.getLikedBy().stream().anyMatch(
                    userRef -> userRef.getId().equals(
                            FirebaseAuthLiveData.getInstance().getAuth().getUid()
                    )
            )
    );
  }

  private void setUserNameView(Post post, PostViewHolder viewHolder) {
    viewHolder.userNameView.setOnClickListener(view -> {
      Fragment fragment = new ProfileFragment();
      Bundle bundle = new Bundle();
      bundle.putString("uid", post.getUid().getId());
      fragment.setArguments(bundle);
      ((FragmentActivity) viewHolder.itemView.getContext()).getSupportFragmentManager()
              .beginTransaction()
              .add(R.id.fragment_container_view, fragment)
              .addToBackStack(this.getClass().getCanonicalName())
              .commit();
    });
  }

  public static class PostViewHolder extends RecyclerView.ViewHolder {
    public ImageView userIcon, imageView, like, unlike, locationPin;
    public TextView userNameView, hashtagsTextView, descriptionView, likeCountTextView, locationTextView;
    public MaterialCardView materialCardView;

    public PostViewHolder(View view) {
      super(view);
      userIcon = view.findViewById(R.id.card_profile_pic);
      imageView = view.findViewById(R.id.card_main_image);
      like = view.findViewById(R.id.like_button);
      unlike = view.findViewById(R.id.unlike_button);

      materialCardView = view.findViewById(R.id.post_card);
      userNameView = view.findViewById(R.id.card_username);
      hashtagsTextView = view.findViewById(R.id.card_hashtags);
      descriptionView = view.findViewById(R.id.card_description);
      likeCountTextView = view.findViewById(R.id.like_count_text);
      locationTextView = view.findViewById(R.id.location);
      locationPin = view.findViewById(R.id.pin);
    }
  }
}