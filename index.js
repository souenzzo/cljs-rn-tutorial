const {name} = require('./app.json');
const React = require('react');
const {main} = require('./cljs/app_native.rn.js');
const {AppRegistry} = require('react-native');


class App extends React.Component {
    render() {
        return main()
    }
}

AppRegistry.registerComponent(
    name,
    () => App);
