package com.easemob.chatuidemo.activity;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.DemoApplication;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.widget.ExpandGridView;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;
import com.easemob.util.NetUtils;

public class GroupDetailsActivity extends Activity {
	private static final String TAG = "GroupDetailsActivity";
	private static final int REQUEST_CODE_ADD_USER = 0;
	private static final int REQUEST_CODE_EXIT = 1;
	private static final int REQUEST_CODE_EXIT_DELETE = 2;
	private ExpandGridView userGridview;
	private String groupId;
	private ProgressBar loadingPB;
	private Button exitBtn;
	private Button deleteBtn;
	private EMGroup group;
	private GridAdapter adapter;
	private int referenceWidth;
	private int referenceHeight;
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_details);

		userGridview = (ExpandGridView) findViewById(R.id.gridview);
		loadingPB = (ProgressBar) findViewById(R.id.progressBar);
		exitBtn = (Button) findViewById(R.id.btn_exit_grp);
		deleteBtn = (Button) findViewById(R.id.btn_exitdel_grp);

		Drawable referenceDrawable = getResources().getDrawable(R.drawable.smiley_add_btn);
		referenceWidth = referenceDrawable.getIntrinsicWidth();
		referenceHeight = referenceDrawable.getIntrinsicHeight();

		// 获取传过来的groupid
		groupId = getIntent().getStringExtra("groupId");
		group = EMGroupManager.getInstance().getGroup(groupId);
		// 如果自己是群主，显示解散按钮
		if (group.getOwner().equals(EMChatManager.getInstance().getCurrentUser())) {
			exitBtn.setVisibility(View.GONE);
			deleteBtn.setVisibility(View.VISIBLE);
		}
		((TextView) findViewById(R.id.group_name)).setText(group.getGroupName());
		adapter = new GridAdapter(this, R.layout.grid, group.getMembers());
		userGridview.setAdapter(adapter);
		// 设置OnTouchListener
		userGridview.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (adapter.isInDeleteMode) {
						adapter.isInDeleteMode = false;
						adapter.notifyDataSetChanged();
						return true;
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (progressDialog == null) { 
				progressDialog = new ProgressDialog(GroupDetailsActivity.this);
				progressDialog.setMessage("正在添加...");
				progressDialog.setCanceledOnTouchOutside(false);
				progressDialog.show();
			}
			switch (requestCode) {
			case REQUEST_CODE_ADD_USER:// 添加群成员
				final String[] newmembers = data.getStringArrayExtra("newmembers");
				addMembersToGroup(newmembers);
				
				break;
			case REQUEST_CODE_EXIT: //退出群
				progressDialog.setMessage("正在退出群...");
				exitGrop();
				break;
			case REQUEST_CODE_EXIT_DELETE: //解散群
				progressDialog.setMessage("正在解散群...");
				deleteGrop();
				break;

			default:
				break;
			}
		}
	}
	
	/**
	 * 点击退出群组按钮
	 * @param view
	 */
	public void exitGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class), REQUEST_CODE_EXIT);

	}

	/**
	 * 点击解散群组按钮
	 * @param view
	 */
	public void exitDeleteGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class).putExtra("deleteToast", getString(R.string.dissolution_group_hint)),
				REQUEST_CODE_EXIT_DELETE);

	}
	
	/**
	 * 退出群组
	 * 
	 * @param groupId
	 */
	private void exitGrop() {
		new Thread(new Runnable() {
			public void run() {
				try {
					EMGroupManager.getInstance().exitFromGroup(groupId, EMChatManager.getInstance().getCurrentUser());
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
							ChatActivity.activityInstance.finish();
						}
					});
				} catch (final EaseMobException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "退出群聊失败: "+e.getMessage(), 1).show();
						}
					});
				}
			}
		}).start();
	}

	/**
	 * 解散群组
	 * 
	 * @param groupId
	 */
	private void deleteGrop() {
		new Thread(new Runnable() {
			public void run() {
				try {
					EMGroupManager.getInstance().exitAndDeleteGroup(groupId);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
							ChatActivity.activityInstance.finish();
						}
					});
				} catch (final EaseMobException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "解散群聊失败: "+e.getMessage(), 1).show();
						}
					});
				}
			}
		}).start();
	}
	
	/**
	 * 增加群成员
	 * @param newmembers
	 */
	private void addMembersToGroup(final String[] newmembers) {
		new Thread(new Runnable() {
			public void run() {
				try {
					EMGroupManager.getInstance().addUsersToGroup(groupId, newmembers);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							adapter.notifyDataSetChanged();
						}
					});
				} catch (final EaseMobException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "添加群成员失败: "+e.getMessage(), 1).show();
						}
					});
				}
			}
		}).start();
	}

	
	/**
	 * 群组成员gridadapter
	 * 
	 * @author admin_new
	 * 
	 */
	private class GridAdapter extends ArrayAdapter<String> {

		private int res;
		public boolean isInDeleteMode;
		private List<String> objects;

		public GridAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			this.objects = objects;
			res = textViewResourceId;
			isInDeleteMode = false;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(res, null);
			}
			final Button button = (Button) convertView.findViewById(R.id.button_avatar);
			// 最后一个item，减人按钮
			if (position == getCount() - 1) {
				button.setText("");
				// 设置成删除按钮
				button.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.smiley_minus_btn, 0, 0);
				// 如果不是创建者，不提供加减人按钮
				if (!group.getOwner().equals(DemoApplication.getInstance().getUserName())) {
					// if current user is not group admin, hide add/remove btn
					convertView.setVisibility(View.INVISIBLE);
				} else { // 显示删除按钮
					if (isInDeleteMode) {
						// 正处于删除模式下，隐藏删除按钮
						convertView.setVisibility(View.INVISIBLE);
					} else {
						// 正常模式
						convertView.setVisibility(View.VISIBLE);
						convertView.findViewById(R.id.badge_delete).setVisibility(View.INVISIBLE);
					}
					button.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							EMLog.d(TAG, "删除按钮被点击");
							isInDeleteMode = true;
							notifyDataSetChanged();
						}
					});
				}
			} else if (position == getCount() - 2) { // 添加群组成员按钮
				button.setText("");
				button.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.smiley_add_btn, 0, 0);
				// 如果不是创建者，不提供加减人按钮
				if (!group.getOwner().equals(DemoApplication.getInstance().getUserName())) {
					// if current user is not group admin, hide add/remove btn
					convertView.setVisibility(View.INVISIBLE);
				} else {
					// 正处于删除模式下,隐藏添加按钮
					if (isInDeleteMode) {
						convertView.setVisibility(View.INVISIBLE);
					} else {
						convertView.setVisibility(View.VISIBLE);
						convertView.findViewById(R.id.badge_delete).setVisibility(View.INVISIBLE);
					}
					button.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							EMLog.d(TAG, "添加按钮被点击");
							// 进入选人页面
							startActivityForResult(
									(new Intent(GroupDetailsActivity.this, GroupPickContactsActivity.class).putExtra("groupId", groupId)),
									REQUEST_CODE_ADD_USER);
						}
					});
				}
			} else { // 普通item，显示群组成员
				final String username = getItem(position);
				button.setText(username);
				convertView.setVisibility(View.VISIBLE);
				button.setVisibility(View.VISIBLE);
				Drawable avatar = getResources().getDrawable(R.drawable.default_avatar);
				avatar.setBounds(0, 0, referenceWidth, referenceHeight);
				button.setCompoundDrawables(null, avatar, null, null);
				// demo群组成员的头像都用默认头像，需由开发者自己去设置头像
				if (isInDeleteMode) {
					// 如果是删除模式下，显示减人图标
					convertView.findViewById(R.id.badge_delete).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.badge_delete).setVisibility(View.INVISIBLE);
				}
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (isInDeleteMode) {
							// 如果是删除自己，return
							if (EMChatManager.getInstance().getCurrentUser().equals(username)) {
								startActivity(new Intent(GroupDetailsActivity.this, AlertDialog.class).putExtra("msg", "不能删除自己"));
								return;
							}
							if (!NetUtils.hasNetwork(getApplicationContext())) {
								Toast.makeText(getApplicationContext(), getString(R.string.network_unavailable), 0).show();
								return;
							}
							EMLog.d("group", "remove user from group:" + username);
							final ProgressDialog deleteDialog = new ProgressDialog(GroupDetailsActivity.this);
							deleteDialog.setMessage("正在移除...");
							deleteDialog.setCanceledOnTouchOutside(false);
							deleteDialog.show();
							new Thread(new Runnable() {

								@Override
								public void run() {
									try {
										// 删除被选中的成员
										EMGroupManager.getInstance().removeUserFromGroup(groupId, username);
										isInDeleteMode = false;
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												deleteDialog.dismiss();
												notifyDataSetChanged();
											}
										});
									} catch (final Exception e) {
										deleteDialog.dismiss();
										runOnUiThread(new Runnable() {
											public void run() {
												Toast.makeText(getApplicationContext(), "删除失败：" + e.getMessage(), 1).show();
											}
										});
									}

								}
							}).start();
						} else {
							// 正常情况下点击user，可以进入用户详情或者聊天页面等等
							// startActivity(new
							// Intent(GroupDetailsActivity.this,
							// ChatActivity.class).putExtra("userId",
							// user.getUsername()));
						}
					}
				});
			}
			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount() + 2;
		}
	}
}