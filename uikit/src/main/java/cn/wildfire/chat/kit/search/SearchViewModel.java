/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class SearchViewModel extends ViewModel {
    private MutableLiveData<SearchResult> resultLiveData = new MutableLiveData<>();

    private Handler workHandler;
    private Handler mainHandler = new Handler();
    private String keyword;
    private boolean isSearching = false;
    private LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();

    public SearchViewModel() {
        init();
    }

//    public void search(String keyword, List<SearchableModule> searchableModules) {
//        if (TextUtils.isEmpty(keyword)) {
//            return;
//        }
//        if (this.keyword != null && this.keyword.equals(keyword)) {
//            return;
//        }
//        this.keyword = keyword;
//        workHandler.post(() -> {
//            boolean found = false;
//            // 搜尋所有 module
//            for (SearchableModule module : searchableModules) {
//                List result = module.searchInternal(keyword);
//                if (keyword.equals(SearchViewModel.this.keyword) && result != null && !result.isEmpty()) {
//                    found = true;
//                    mainHandler.post(() -> resultLiveData.setValue(new SearchResult(module, result)));
//                }
//            }
//
//            // 沒有搜尋到相關資料
//            if (keyword.equals(SearchViewModel.this.keyword) && !found) {
//                mainHandler.post(() -> resultLiveData.setValue(null));
//            }
//            this.keyword = null;
//        });
//    }

    public void search(String keyword, List<SearchableModule> searchableModules) {
        LogHelper.e("search", "search text = " + keyword);
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        // 搜寻资料放进队列
        try {
            linkedBlockingQueue.put(keyword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isSearching) {
            return;
        }
        // 如果没有搜寻任务 则进入搜寻
        searchTask(searchableModules);
    }

    private void searchTask(List<SearchableModule> searchableModules) {
        String searchText = linkedBlockingQueue.poll();
        if (searchText == null) {
            return;
        }
        workHandler.post(new SearchTask(searchableModules, searchText));
    }

    public LiveData<List<Message>> searchMessage(Conversation conversation, String keyword) {
        MutableLiveData<List<Message>> result = new MutableLiveData<>();
        workHandler.post(() -> {
            List<Message> messages = ChatManager.Instance().searchMessage(conversation, keyword, true, 100, 0);
            result.postValue(messages);
        });
        return result;
    }

    public MutableLiveData<SearchResult> getResultLiveData() {
        return resultLiveData;
    }

    private void init() {
        if (workHandler == null) {
            HandlerThread thread = new HandlerThread("search");
            thread.start();
            workHandler = new Handler(thread.getLooper());
        }
    }

    class SearchTask implements Runnable {
        private String searchText;
        private List<SearchableModule> searchableModules;

        SearchTask(List<SearchableModule> searchableModules, String text) {
            this.searchableModules = searchableModules;
            searchText = text;
        }

        @Override
        public void run() {
            // 没有其他需要搜寻的资料
            if (searchText == null) {
                return;
            }
            isSearching = true;
            boolean found = false;
            LogHelper.e("search", "start search = " + searchText);

            // 搜尋所有 module
            for (SearchableModule module : searchableModules) {
                List result = module.searchInternal(searchText);
                if (result != null && !result.isEmpty()) {
                    found = true;
                    mainHandler.post(() -> resultLiveData.setValue(new SearchResult(module, result)));
                }
            }

            // 沒有搜尋到相關資料
            if (!found) {
                mainHandler.post(() -> resultLiveData.setValue(null));
            }
            isSearching = false;
            searchTask(searchableModules);
        }
    }
}
