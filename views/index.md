---
layout: default
---
Views in catkins
================

There are many useful extended views provided by this project. But you can find projects on GitHub that provided only one or two custom views like SlidingMenu and PullRefreshListView. What I want to do is not just combie them into one project, but to write every view with more customizable and can be used in general situation without mush more modification on the source file. Also we suggest to extended them instead of rewriting the source files to achieve some specific features.

Views useage
============

Flip3DLayout
------------

It can flip from one view to another one and has a 3D animation when do flipping. In fact this is a container ViewGroup that holds only two children. Any more child view in Flip3DLayout will not display.

You can define it in an xml layout file like this:

{% highlight xml %}
<LinearLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:catkins="http://schemas.android.com/apk/res/com.simon.catkins.demo"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

    <com.simon.catkins.views.Flip3DLayout
            android:id="@+id/flip_3d_layout"
            android:orientation="vertical"
            android:layout_width="match_parent"
            android:layout_weight="1"
            android:layout_height="0dp"
            catkins:from="@+id/from"
            catkins:to="@+id/to">

        <View
                android:id="@+id/from"
                android:background="@android:color/holo_blue_dark"
                android:layout_width="match_parent"
                android:layout_height="match_parent"/>

        <View
                android:id="@+id/to"
                android:background="@android:color/holo_red_light"
                android:layout_width="match_parent"
                android:layout_height="match_parent"/>
    </com.simon.catkins.views.Flip3DLayout>
</LinearLayout>
{% endhighlight %}

Flip3DLayout has two custom attributes: **from** and **to**. You should specific a id value that reference to a view in its child node. So when inflating this xml, Flip3DLayout will try to find the views with these two ids from its children.


