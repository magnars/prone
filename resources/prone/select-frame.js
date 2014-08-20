/*global dome */

(function (d) {

  function closestWithClass(element, className) {
    if (element && d.cn.has(className, element)) {
      return element;
    }
    return closestWithClass(element.parentNode, className);
  }

  function delegateByClass(className, parent, event, handler) {
    d.on(parent, event, function (e) {
      var element = closestWithClass(e.target, className);
      if (element) {
        handler.call(null, element, e);
      }
    });
  }

  var selectFrame = function (id) {
    var frameEntry = d.id("frame_entry_" + id);
    var frameInfo = d.id("frame_info_" + id);
    d.cn.add("selected", frameEntry);
    d.cn.rm("hidden", frameInfo);
  };

  var deselectFrame = function (id) {
    var frameEntry = d.id("frame_entry_" + id);
    var frameInfo = d.id("frame_info_" + id);
    d.cn.rm("selected", frameEntry);
    d.cn.add("hidden", frameInfo);
  };

  var selectedFrameId = 0;
  selectFrame(0);

  delegateByClass("frame", d.id("frames"), "click", function (element) {
    var frameId = element.getAttribute("data-frame-id");
    deselectFrame(selectedFrameId);
    selectFrame(frameId);
    selectedFrameId = frameId;
  });

}(dome));
