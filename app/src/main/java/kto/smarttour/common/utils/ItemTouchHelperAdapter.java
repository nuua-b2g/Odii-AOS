package kto.smarttour.common.utils;

public interface ItemTouchHelperAdapter {
    /**
     * Called when item is moved
     *
     * @ The starting point of the item from which the param fromPosition is operated
     * @ The end point of the item to which the param toPosition is operated
     */
    void onItemMove(int fromPosition, int toPosition);

    /**
     * Called when item is sideslipped
     *
     * @ The position of the item whose param position is sideslipped
     */
    void onItemDismiss(int position);


	void OnItemMoveComplete();
}
