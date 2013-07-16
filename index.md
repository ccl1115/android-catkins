---
layout: default
---
This is an android library project to enhance the UI development experience. With Catkins you can easily structure an
app with a slide view or obtain some customizable widgets.

But now it's under heavy development and has many features to accomplish.


Sub projects
============

catkins-lib
-----------

The library project you need to include to your android project if you want use catkins.

catkins-it
----------

The instrumentation test project. It actually tests catkins-demo project, because Android test framework cannot test a
library project.

catkins-demo
------------

A standard android project to demonstrate catkins usages.

Maven
=====

We use Maven to for project management. For how to build and test android project using Maven, you can check out the [maven-android-plugin](http://github.com/jayway/maven-android-plugin) and the [maven-android-sdk-deployer](http://github.com/jayway/mosahua/maven-android-sdk-deployer).

If you don't get familiar with Maven or even don't know what Maven is, here it's [documentation](http://maven.apache.org).

Custom views
============

This library provides some useful view components to help developers improve user experience.

* Flip3DLayout
* HorizontalTranslateLayout
* PinnedHeaderListView
* RefreshView
* StackLayout
* IndicatorView for ViewPager
* HorizontalListView forked from github.com/MeetMe/Android-HorizontalListView


Licenses
========

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
