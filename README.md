#AndroidGoogleImages#

This is a Test project using the deprecated [Google Images Ajax API](https://developers.google.com/image-search/) to display images in a 3 column grid.

###Libraries###
* [Android Asynchronous HTTP Client](http://loopj.com/android-async-http/): Used to perform the calls to the Google Images API. 
* [Android Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader): Used to download the images and cache them both in memory and in disk

###Notes###
* Google API allows a maximum of 8 images per request. Since I have 3 columns, I am downloading the images 6 by 6. If I wanted to download 8, I would have to make some changes to the ImageAdapter, to add a possible empty view in the last column, on top of the 3 items I added (a new row) for the loading views. See ImageAdapter for code and comments.

* Google API only allows 8 pages, so total number of images I request is 8*6 = 48 (Not reaching the suggested 50 limit). 

* I could hace implemented caches myself. In that case, I would probably have used a LRUCache caching bitmaps for memory. writing to that cache could be done in bg, always synchronizing the calls. Reading should happen in the Adapter and UI thread. For disk cache, I could store the images in the External Drive, and read them in a bg thread.

* Note on Fragments: A better implementation would be to have the GridView in a Fragment, so that orientation changes wouldn't destroy it, and it could be used in other views/devices. I didn't implement Fragments just for simplicity. 

* In ImageAdapter, I didn't use XML layouts to create my views for simplicity. They are simply ImageView's, and even though it is good practice to have design separeted from the logic, it is also known that inflating XML layouts is expensive and in an adapter, you want to be as fast as possible.


