##Description
Android app for sentiment analysis and summary of POLITICO's congress articles.


##How it works
It mainly uses the NLP functionalities of the [Intellexer API](www.intellexer.com)

The stream of the app is as follow:  
1. Fetch the POLITICO's congress [RSS feed](http://www.politico.com/rss/congress.xml)  
2. Parse the RSS feed and display article titles  

*When an article is clicked:*  
3. Download article's HTML page  
4. Parse the HTML page to only extract article's text for better performance  
5. Call summarize method of [Intellexer API](www.intellexer.com) with article's text  
6. Call sentiment analyis of [Intellexer API](www.intellexer.com) with article's text  
7. Display the summary on a new screen  
8. Display sentiment analaysis with [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) library  

##Improvements
* improve stability by checking the network connectivity (as of today the app might crash if the phone can't connect to network)
* improve speed by delegating the XML parsing, HTML parsing and Intellexer API call to a third-tier API
* improve quality of content by specifically training a NLP machine learning with political contexts


