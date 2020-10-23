/*****************************************************************************************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 ****************************************************************************************************************************************************/
AjaxFranceLabs.PagerModule = AjaxFranceLabs.AbstractModule.extend({

  // Variables

  source : null,

  prevLabel : '<i class="fas fa-chevron-left"></i>',

  nextLabel : '<i class="fas fa-chevron-right"></i>',

  firstLabel : '<i class="fas fa-chevron-left"></i><i class="fas fa-chevron-left"></i>',

  lastLabel : '<i class="fas fa-chevron-right"></i><i class="fas fa-chevron-right"></i>',

  nbElmToDisplay : 10,

  nbPage : 0,

  nbPageDisplayed : 4,

  nbElements : 0,

  fastNavigationAlwaysVisible : false,

  display : null,

  pageSelected : 0,

  type : 'pager',

  // Methods

  init : function() {
    if (!this.initialized) {
      this.initialized = true;

      var self = this, elm = $(this.elm);
      this.nbPageDisplayed = (this.nbPageDisplayed < 4) ? 4 : this.nbPageDisplayed;
      elm.append('<div></div>');
      elm.find('div:last').addClass('pagerModule').addClass('module').append('<span class="go_f fn fl"></span>').append(
          '<span class="go_p fn pn"></span>').append('<div class="pages"></div>').append('<span class="go_n fn pn"></span>').append(
          '<span class="go_l fn fl"></span>');
      elm.find('.go_f').append('<span>' + this.firstLabel + '</span>');
      elm.find('.go_p').append('<span>' + this.prevLabel + '</span>');
      elm.find('.go_n').append('<span>' + this.nextLabel + '</span>');
      elm.find('.go_l').append('<span>' + this.lastLabel + '</span>');
      elm.find('.fn').click(function() {
        if ($(this).hasClass('go_f'))
          self.pageSelected = 0;
        else if ($(this).hasClass('go_l'))
          self.pageSelected = self.nbPage - 1;
        else if ($(this).hasClass('go_p'))
          self.pageSelected = (self.pageSelected === 0) ? 0 : self.pageSelected - 1;
        else
          self.pageSelected = (self.pageSelected === self.nbPage - 1) ? self.pageSelected : self.pageSelected + 1;
        self.clickHandler();
      });
      if (this.display === null)
        this.display = $(this.elm).find('.fn').css('display');
    }
  },

  updatePages : function() {

    var self = this, elm = $(this.elm);
    this.nbElements = (this.source !== null) ? this.source.children().length : this.nbElements;
    this.nbPage = Math.ceil(this.nbElements / this.nbElmToDisplay);
    if (this.nbPage <= 1) {
      $(this.elm).find('.pagerModule').hide();
      return false;
    }
    $(this.elm).find('.pagerModule').show();
    elm.find('.pages').empty();
    var firstPage = this.pageSelected - 10;
    if (firstPage < 0) {
      firstPage = 0;
    }
    for (var p = firstPage; p < firstPage + 20 && p < this.nbPage; p++) {
      $(this.elm).find('.pages').append('<span class="page" page="' + p + '"><span>' + (p + 1) + '</span></span>');
      if (p === this.pageSelected)
        $(this.elm).find('.page:last').addClass('selected');
    }

    elm.find('.page').click(function() {
      self.pageSelected = parseInt($(this).attr('page'));
      self.clickHandler();
    });
    elm.find('.page.selected').unbind('click');
    if (!this.fastNavigationAlwaysVisible) {
      if (this.pageSelected === 0)
        elm.find('.go_f, .go_p').css('display', 'none');
      else
        $(this.elm).find('.go_f, .go_p').css('display', this.display);
      if (this.pageSelected === this.nbPage - 1)
        elm.find('.go_n, .go_l').css('display', 'none');
      else
        elm.find('.go_n, .go_l').css('display', this.display);
    }
    elm.find('.page').css('display', 'none');
    elm.find('.page[page=' + this.pageSelected + ']').addClass('selected disp');
    // Add class 'disp' because the old code version relied on the ':visible' filter to find .page elements
    // but if the pager module is hidden at this point it breaks everything
    // By relying on a class, the behavior of the following code will be correct in any cases
    elm.find('.page.selected').css('display', this.display).prev().addClass('disp').css('display', this.display);
    for (var p = $(this.elm).find('.page.disp').length; p < this.nbPageDisplayed; p++) {
      // Display the page numbers one by one till the nbPageDisplayed (which should be renamed as 'MaxPagesToDisplay') is reached
      elm.find('.page.disp:last').next().addClass('disp').css('display', this.display);
    }
    // If the number of currently displayed pages is below the max allowed number (nbPageDisplayed),
    // then display any existing previous pages till the max allowed number is reached
    // For example if the selected page is 11 and the current diplayed pages are 11,12 and 13, if the max allowed pages to display is 10
    // then we can display the pages 4,5,6,7,8,9 and 10 in addition
    if (elm.find('.page.disp').length < this.nbPageDisplayed) {
      for (var p = $(this.elm).find('.page.disp').length; p < this.nbPageDisplayed; p++) {
        elm.find('.page.disp:first').prev().addClass('disp').css('display', this.display);
      }
    }
    var width = 10;
    for (var p = 0; p < this.nbPageDisplayed; p++) {
      width += $(this.elm).find('.page.disp:eq(' + p + ')').outerWidth(true);
    }
    elm.find('.pages').width(width);
    AjaxFranceLabs.clearMultiElementClasses(elm.find('.page'));
    AjaxFranceLabs.addMultiElementClasses(elm.find('.page.disp'));
  },

  clickHandler : function() {
    this.updateList();
    this.updatePages();
  },

  updateList : function() {
  }
});
