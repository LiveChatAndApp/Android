package cn.wildfire.chat.kit.withdraw;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.PaymentInfo;

public class BankListAdapter extends RecyclerView.Adapter<BankListAdapter.BankViewHolder> {
    private List<PaymentInfo> paymentInfoList;
    private BankCardDeleteListener listener;

    public BankListAdapter() {
        if (paymentInfoList == null) {
            paymentInfoList = new ArrayList<>();
        }
    }

    public void setListener(BankCardDeleteListener bankCardDeleteListener) {
        listener = bankCardDeleteListener;
    }

    public void setData(List<PaymentInfo> list) {
        if (paymentInfoList == null) {
            paymentInfoList = new ArrayList<>();
        } else {
            paymentInfoList.clear();
        }
        paymentInfoList.addAll(list);
        notifyDataSetChanged();
    }

    public void deleteData(PaymentInfo info) {
        if (paymentInfoList == null) {
            return;
        }
        paymentInfoList.remove(info);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bank_list, parent, false);
        return new BankViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BankViewHolder holder, int position) {
        PaymentInfo info = paymentInfoList.get(position);
        holder.bind(info);
    }

    @Override
    public int getItemCount() {
        return paymentInfoList.size();
    }

    class BankViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.customNameTextView)
        MaterialTextView customName;
        @BindView(R2.id.bankNameTextView)
        MaterialTextView bankName;
        @BindView(R2.id.bankAccountTextView)
        MaterialTextView account;
        @BindView(R2.id.ownerNameTextView)
        MaterialTextView owner;
        @BindView(R2.id.deleteImageView)
        ImageView deleteImageView;

        @OnClick(R2.id.deleteImageView)
        void BankCardDeleteClick() {
            if (listener != null) {
                listener.onBankCardDeleteClick(paymentInfo);
            }
        }

        private PaymentInfo paymentInfo;

        public BankViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(PaymentInfo info) {
            paymentInfo = info;
            customName.setText(info.name);
            bankName.setText(info.bankName);
            account.setText(info.bankCardNumber);
            owner.setText(info.ownerName);
        }
    }

    interface BankCardDeleteListener {
        void onBankCardDeleteClick(PaymentInfo info);
    }
}