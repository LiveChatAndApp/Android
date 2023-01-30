package cn.wildfire.chat.kit.user.wallet;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.recharge.RechargeUploadActivity;
import cn.wildfirechat.model.RechargeResultInfo;
import cn.wildfirechat.model.WalletOrderInfo;

public class WalletDetailAdapter extends RecyclerView.Adapter<WalletDetailAdapter.ViewHolder> {
    private List<WalletOrderInfo> dataList = new ArrayList<>();
    private SimpleDateFormat dateFormat;

    public WalletDetailAdapter() {
    }

    public void setDataList(List<WalletOrderInfo> list) {
        if (list == null) {
            return;
        }
        dataList.clear();
        dataList.addAll(list);
        notifyItemRangeChanged(0, dataList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_detail, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.statusTextView)
        MaterialTextView statusTextView;
        @BindView(R2.id.typeTextView)
        MaterialTextView typeTextView;
        @BindView(R2.id.amountTextView)
        MaterialTextView amountTextView;
        @BindView(R2.id.orderIdTextView)
        MaterialTextView orderIdTextView;
        @BindView(R2.id.dateTextView)
        MaterialTextView dateTextView;
        @BindView(R2.id.submitButton)
        Button submitButton;

        WalletOrderInfo info;
        Context context = null;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            ButterKnife.bind(this, itemView);
        }

        public void bind(WalletOrderInfo info) {
            this.info = info;
            String status = "";
            String type = "";
            String orderId = "";
            String amount = "";
            int statusColor = R.color.blueBtn;
            switch (info.type) {
                case 1: // 充值
                    amount = context.getString(R.string.recharge_amount) + " : " + info.amount;
                    type = context.getString(R.string.recharge_text);
                    break;
                case 2:
                    amount = context.getString(R.string.withdraw_money) + " : " + info.amount;
                    type = context.getString(R.string.withdraw_text);
                    break;
            }

            switch (info.status) {
                case 0:
                    statusColor = R.color.blueBtn;
                    status = context.getString(R.string.order_status_not_finish_text);
                    break;
                case 1:
                    status = context.getString(R.string.order_status_wait_text);
                    statusColor = R.color.blueBtn;
                    break;
                case 2:
                    statusColor = R.color.green5;
                    status = context.getString(R.string.order_status_success_text);
                    break;
                case 3:
                    statusColor = R.color.gray23;
                    status = context.getString(R.string.order_status_reject_text);
                    break;
                case 4:
                    statusColor = R.color.gray23;
                    status = context.getString(R.string.order_status_cancel_text);
                    break;
                case 5:
                    statusColor = R.color.red9;
                    status = context.getString(R.string.order_status_over_time_text);
                    break;
            }
            orderId = context.getString(R.string.order_id_text) + " : " + info.orderCode;

            statusTextView.setText(status);
            statusTextView.setTextColor(statusTextView.getResources().getColor(statusColor));
            typeTextView.setText(type);
            amountTextView.setText(amount);
            orderIdTextView.setText(orderId);
            dateTextView.setText(getTime(info.createTime));
            submitButton.setVisibility(info.status == 0 ? View.VISIBLE : View.GONE);
        }

        private String getTime(long time) {
            if (dateFormat == null) {
                Locale mDefault = Locale.getDefault();
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", mDefault);
            }
            return dateFormat.format(time);
        }

        @OnClick(R2.id.submitButton)
        public void uploadClick() {
            if (context == null)
                return;
            Intent intent = new Intent(context, RechargeUploadActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(context,
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            intent.putExtra("RechargeChannel", info.rechargeChannel);
            RechargeResultInfo rechargeResultInfo = new RechargeResultInfo();
            rechargeResultInfo.setId(info.id);
            rechargeResultInfo.setMethod(info.rechargeChannel.paymentMethod);
            rechargeResultInfo.setOrderCode(info.orderCode);
            intent.putExtra("RechargeResultInfo", rechargeResultInfo);
            context.startActivity(intent, bundle);
        }
    }
}
