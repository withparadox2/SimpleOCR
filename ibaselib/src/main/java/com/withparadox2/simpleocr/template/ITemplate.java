package com.withparadox2.simpleocr.template;

import android.text.TextWatcher;

public interface ITemplate {
    public void setTitle(String title);

    public void setAuthor(String author);

    public void setContent(String content);

    public void setDate(String date);

    public boolean renderBitmapAndSave(String filePath);

    public int getEditSelection();

    public CharSequence getEditTextContent();

    public void setEditSelection(int selection);

    public void setContentTextWatcher(TextWatcher watcher);

    public void setCallback(Callback callback);
}
