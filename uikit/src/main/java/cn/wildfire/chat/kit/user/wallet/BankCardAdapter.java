package cn.wildfire.chat.kit.user.wallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.PaymentInfo;

public class BankCardAdapter extends BaseAdapter {
    private List<PaymentInfo> paymentInfos;

    public BankCardAdapter(List<PaymentInfo> list) {
        paymentInfos = list;
    }

    @Override
    public int getCount() {
        return paymentInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return paymentInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_bank, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (MaterialTextView) convertView.findViewById(R.id.text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        try {
            if (position < 0) {
                position = 0;
            }
            PaymentInfo info = paymentInfos.get(position);
            viewHolder.text.setText(info.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertView;
    }

    class ViewHolder {
        MaterialTextView text;
    }
}
