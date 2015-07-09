package id.phone.demo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import id.phone.demo.ui.InfoDialog;
import id.phone.sdk.PhoneId;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserResponse;
import id.phone.sdk.ui.view.LoginButton;


public class MainActivity extends FragmentActivity
{
	public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //ok we've got some data from the PhoneId SDK
        if (resultCode == RESULT_OK && requestCode == LoginButton.LOGIN_REQUEST_CODE && data != null) {
            ((PlaceholderFragment)
				getSupportFragmentManager().findFragmentById(R.id.container))
				.showUserInfo(data.getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        Button btnLoginAccountManager;
		ViewGroup layoutButtons;
		Button btnShowAccessToken;
		Button btnShowUserInfo;
		Button btnUploadContacts;

        public PlaceholderFragment() {
        }

		private BroadcastReceiver phoneIdEventsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent)
			{
				if (intent != null)
				{
					if (PhoneId.ACTION_LOGGED_IN.equals(intent.getAction()))
						phoneIdEventListener.onLoggedIn(
							intent.getStringExtra(PhoneId.ARG_TOKEN_TYPE)
							, intent.getStringExtra(PhoneId.ARG_ACCESS_TOKEN)
							, intent.getStringExtra(PhoneId.ARG_REFRESH_TOKEN)
						);
					else if (PhoneId.ACTION_LOGGED_OUT.equals(intent.getAction()))
						phoneIdEventListener.onLoggedOut();
					else if (PhoneId.ACTION_USER_PROFILE.equals(intent.getAction()))
						phoneIdEventListener.onUserProfile(
							intent.getStringExtra(PhoneId.ARG_USER_PROFILE));
					else if (PhoneId.ACTION_CONTACTS_UPLOADED.equals(intent.getAction()))
						phoneIdEventListener.onContactsUploaded(
							intent.getStringExtra(PhoneId.ARG_RESPONSE));
					else if (PhoneId.ACTION_ERROR.equals(intent.getAction()))
						phoneIdEventListener.onError(
							intent.getStringExtra(PhoneId.ARG_ERROR_KIND)
							, intent.getStringExtra(PhoneId.ARG_ERROR_CODE)
							, intent.getStringExtra(PhoneId.ARG_ERROR_MESSAGE)
						);
				}
			}
		};

		@Override
		public void onAttach(Activity activity)
		{
			super.onAttach(activity);

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(PhoneId.ACTION_LOGGED_IN);
			intentFilter.addAction(PhoneId.ACTION_LOGGED_OUT);
			intentFilter.addAction(PhoneId.ACTION_USER_PROFILE);
			intentFilter.addAction(PhoneId.ACTION_CONTACTS_UPLOADED);
			intentFilter.addAction(PhoneId.ACTION_ERROR);
			LocalBroadcastManager.getInstance(activity).registerReceiver(phoneIdEventsReceiver, intentFilter);
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			if (getActivity() != null)
				LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(phoneIdEventsReceiver);
		}

			@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			btnLoginAccountManager = (Button)rootView.findViewById(R.id.btnLoginAccountManager);
			layoutButtons = (ViewGroup)rootView.findViewById(R.id.layoutButtons);
			btnShowAccessToken = (Button)rootView.findViewById(R.id.btnShowAccessToken);
			btnShowUserInfo = (Button)rootView.findViewById(R.id.btnShowUserInfo);
			btnUploadContacts = (Button)rootView.findViewById(R.id.btnUploadContacts);

			btnLoginAccountManager.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					try
					{
						PhoneId.getInstance().getAccessToken(getActivity(), new PhoneId.TokenResponseCallback() {
							@Override
							public void tokenResponseDelivered(TokenResponse tokenResponse)
							{
								InfoDialog.newInstance(R.string.msg_access_token, tokenResponse.toString())
									.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
							}
						});
					}
					catch (Exception ex)
					{
						InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
							.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
					}
				}
			});

			try
			{
				layoutButtons
					.setVisibility(PhoneId.getInstance().isLoggedIn() ? View.VISIBLE : View.GONE);
			}
			catch (Exception ex)
			{
				InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
					.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
			}

			btnShowAccessToken.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						TokenResponse response;
						if (getActivity() != null &&
							(response = PhoneId.getInstance().getAccessToken(getActivity(), null)) != null)
						{
							InfoDialog.newInstance(R.string.msg_access_token, response.toString())
								.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
						}
					}
					catch (Exception ex)
					{
						InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
							.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
					}
				}
			});

			btnShowUserInfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					try
					{
						UserResponse response;
						if (getActivity() != null &&
							(response = PhoneId.getInstance().getUser()) != null)
						{
							InfoDialog.newInstance(R.string.msg_user_info, response.toString())
								.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
						}
					}
					catch (Exception ex)
					{
						InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
							.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
					}
				}
			});

			btnUploadContacts.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					try
					{
						if (getActivity() != null)
						{
							PhoneId.getInstance().uploadContactsToServer();
							btnUploadContacts.setText(R.string.btn_uploading_contacts);
							btnUploadContacts.setEnabled(false);
						}
					}
					catch (Exception ex)
					{
						InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
							.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
					}
				}
			});

            return rootView;
        }

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data)
		{
			if( resultCode == RESULT_CANCELED)
				return;
		}

        public void showUserInfo(String userInfo)
        {
            ((TextView) getView().findViewById(R.id.txtUserInfo)).setText(userInfo);
        }


        private class PhoneIdEventListener
        {
            public void onLoggedIn(String tokenType, String accessToken, String refreshToken)
            {
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							layoutButtons.setVisibility(View.VISIBLE);
							btnUploadContacts.setEnabled(true);
							btnUploadContacts.setText(R.string.btn_upload_contacts);
						}
					});
				}

            }

            public void onLoggedOut()
            {
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							layoutButtons.setVisibility(View.GONE);
						}
					});
				}
				showUserInfo(null);
            }

            public void onUserProfile(final String jsonUserProfile)
            {
                if (getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							showUserInfo(jsonUserProfile);
						}
					});
                }
            }

			public void onContactsUploaded(final String response)
			{
				if (getActivity() != null)
				{
					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							btnUploadContacts.setEnabled(true);
							btnUploadContacts.setText(R.string.btn_upload_contacts);
							InfoDialog.newInstance(R.string.msg_contacts_uploaded, response)
								.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
						}
					});
				}

			}

            public void onError(String code, final String message, final String kind)
            {
                if (getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							showUserInfo(kind + ": " + message);
							InfoDialog.newInstance(R.string.phid_error, kind + ": " + message)
								.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
						}
					});
                }
            }
        };
		private PhoneIdEventListener phoneIdEventListener = new PhoneIdEventListener();
    }
}
