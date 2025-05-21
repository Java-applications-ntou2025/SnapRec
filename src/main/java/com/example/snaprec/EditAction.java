package com.example.snaprec;

public interface EditAction {
    void apply(VideoEditState state);
    void undo(VideoEditState state);
}

