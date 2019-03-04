const {name} = require('./app.json');
const React = require('react');
// const {main} = require('./cljs/app_native.rn.js');
const {Text, AppRegistry} = require('react-native');


class App extends React.Component {
    render() {
        return React.createElement(Text, {}, "Hello")
    }
}

AppRegistry.registerComponent(
    name,
    () => App);
